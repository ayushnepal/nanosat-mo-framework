/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
 * ----------------------------------------------------------------------------
 * Licensed under the European Space Agency Public License, Version 2.0
 * You may not use this file except in compliance with the License.
 *
 * Except as expressly set forth in this License, the Software is provided to
 * You on an "as is" basis and without warranties of any kind, including without
 * limitation merchantability, fitness for a particular purpose, absence of
 * defects or errors, accuracy or non-infringement of intellectual property rights.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * ----------------------------------------------------------------------------
 */
package esa.mo.sm.impl.provider;

import esa.mo.sm.impl.util.ClosingAppListener;
import esa.mo.com.impl.consumer.EventConsumerServiceImpl;
import esa.mo.com.impl.util.COMServicesProvider;
import esa.mo.com.impl.util.DefinitionsManager;
import esa.mo.com.impl.util.HelperArchive;
import esa.mo.com.impl.util.HelperCOM;
import esa.mo.common.impl.util.HelperCommon;
import esa.mo.helpertools.connections.ConfigurationProviderSingleton;
import esa.mo.helpertools.connections.SingleConnectionDetails;
import esa.mo.helpertools.helpers.HelperMisc;
import esa.mo.helpertools.misc.Const;
import esa.mo.sm.impl.util.OSValidator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.ccsds.moims.mo.com.COMHelper;
import org.ccsds.moims.mo.com.archive.structures.ArchiveDetails;
import org.ccsds.moims.mo.com.archive.structures.ArchiveDetailsList;
import org.ccsds.moims.mo.com.structures.ObjectId;
import org.ccsds.moims.mo.com.structures.ObjectType;
import org.ccsds.moims.mo.common.directory.structures.AddressDetailsList;
import org.ccsds.moims.mo.common.directory.structures.ProviderSummaryList;
import org.ccsds.moims.mo.common.directory.structures.ServiceCapabilityList;
import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.provider.MALInteraction;
import org.ccsds.moims.mo.mal.structures.ElementList;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.LongList;
import org.ccsds.moims.mo.mal.structures.Subscription;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.softwaremanagement.appslauncher.AppsLauncherHelper;
import org.ccsds.moims.mo.softwaremanagement.appslauncher.provider.StopAppInteraction;
import org.ccsds.moims.mo.softwaremanagement.appslauncher.structures.AppDetails;
import org.ccsds.moims.mo.softwaremanagement.appslauncher.structures.AppDetailsList;

/**
 *
 * @author Cesar Coelho
 */
public class AppsLauncherManager extends DefinitionsManager
{

  private static final int APP_STOP_TIMEOUT = 5000;
  private static final Logger LOGGER = Logger.getLogger(AppsLauncherManager.class.getName());

  private final OSValidator osValidator = new OSValidator();

  private static final String FOLDER_LOCATION_PROPERTY
      = "esa.mo.sm.impl.provider.appslauncher.FolderLocation";
  private static final String DEFAULT_APPS_FOLDER_PATH
      = ".." + File.separator + ".." + File.separator + "apps";
  /**
   * Location of the apps folder, relative to the MO Supervisor
   */
  private File appsFolderPath;
  private final HashMap<Long, ProcessExecutionHandler> handlers
      = new HashMap<>();

  private AtomicLong uniqueObjIdDef; // Counter

  public AppsLauncherManager(COMServicesProvider comServices)
  {
    super(comServices);

    // If there is a property for that, then use it!!
    if (System.getProperty(FOLDER_LOCATION_PROPERTY) != null) {
      appsFolderPath = new File(System.getProperty(FOLDER_LOCATION_PROPERTY));
    } else {
      LOGGER.log(Level.INFO,
          "Property {0} not set. Using default apps directory '{1}'",
          new Object[]{FOLDER_LOCATION_PROPERTY, DEFAULT_APPS_FOLDER_PATH});
      appsFolderPath = new File(DEFAULT_APPS_FOLDER_PATH);
    }

    try {
      AppsLauncherHelper.init(MALContextFactory.getElementFactoryRegistry());
    } catch (MALException ex) {
      // Apps Launcher service helpers were likely initialized by a different class
    }

    if (super.getArchiveService() == null) {  // No Archive?
      this.uniqueObjIdDef = new AtomicLong(0);
    } else {
      // With Archive...
    }

  }

  protected AppDetailsList getAll()
  {
    return (AppDetailsList) this.getAllDefs();
  }

  @Override
  public Boolean compareName(Long objId, Identifier name)
  {
    return this.get(objId).getName().equals(name);
  }

  @Override
  public ElementList newDefinitionList()
  {
    return new AppDetailsList();
  }

  public AppDetails get(Long input)
  {
    return (AppDetails) this.getDef(input);
  }

  protected Long add(final AppDetails definition, final ObjectId source, final URI uri)
  {
    Long objId = null;
    Long related = null;

    if (definition.getExtraInfo() != null) {
      try { // Read the provider.properties of the app
        File fileProps = new File(appsFolderPath.getCanonicalPath() + File.separator + definition.
            getName().getValue() + File.separator + definition.getExtraInfo());

        Properties props = HelperMisc.loadProperties(fileProps.getCanonicalPath());

        // Look up for apid
        String apidString = (String) props.get(HelperMisc.PROPERTY_APID);
        int apid = (apidString != null) ? Integer.parseInt(apidString) : 0;
        objId = new Long(apid);
      } catch (MalformedURLException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }

    if (super.getArchiveService() == null) {
      if (objId == null) {
        objId = uniqueObjIdDef.incrementAndGet();
      }
      this.addDef(objId, definition);
      return objId;
    } else {
      AppDetailsList defs = new AppDetailsList();
      defs.add(definition);

      try {
        final ArchiveDetailsList archDetails = HelperArchive.generateArchiveDetailsList(related,
            source, uri);
        archDetails.get(0).setInstId(objId);

        LongList objIds = super.getArchiveService().store(
            true,
            AppsLauncherHelper.APP_OBJECT_TYPE,
            ConfigurationProviderSingleton.getDomain(),
            archDetails,
            defs,
            null);

        if (objIds.size() == 1) {
          this.addDef(objIds.get(0), definition);
          return objIds.get(0);
        }
      } catch (MALException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      } catch (MALInteractionException ex) {
        if (ex.getStandardError().getErrorNumber().equals(COMHelper.DUPLICATE_ERROR_NUMBER)) {
          LOGGER.log(Level.WARNING,
              "Error while adding new App: {0}! "
              + "The App COM object with objId: {1} already exists in the Archive!",
              new Object[]{definition.getName().toString(), objId});

          return objId;
        } else {
          LOGGER.log(Level.SEVERE, null, ex);
        }
      }
    }

    return null;
  }

  protected boolean update(final Long objId, final AppDetails definition,
      final MALInteraction interaction)
  { // requirement: 3.3.2.5
    Boolean success = this.updateDef(objId, definition);

    if (super.getArchiveService() != null) {  // It should also update on the COM Archive
      try {
        AppDetailsList defs = new AppDetailsList();
        defs.add(definition);
        final IdentifierList domain = ConfigurationProviderSingleton.getDomain();

        ArchiveDetails archiveDetails = HelperArchive.getArchiveDetailsFromArchive(super.
            getArchiveService(),
            AppsLauncherHelper.APP_OBJECT_TYPE, domain, objId);

        ArchiveDetailsList archiveDetailsList = new ArchiveDetailsList();
        archiveDetailsList.add(archiveDetails);

        super.getArchiveService().update(
            AppsLauncherHelper.APP_OBJECT_TYPE,
            domain,
            archiveDetailsList,
            defs,
            interaction);
      } catch (MALException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
        return false;
      } catch (MALInteractionException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
        return false;
      }
    }

    return success;
  }

  protected boolean delete(Long objId)
  {
    return this.deleteDef(objId);
  }

  protected boolean refreshAvailableAppsList(final URI providerURI)
  {
    // Go to all the "apps folder" and check if there are new folders
    // get all the files from a directory
    File[] fList = appsFolderPath.listFiles();

    if (fList == null) {
      LOGGER.log(Level.SEVERE,
          "The directory could not be found: {0}", appsFolderPath.toString());

      return false;
    }

    boolean anyChanges = false;
    AppDetailsList apps = new AppDetailsList();

    for (File folder : fList) { // Roll all the apps inside the apps folder
      if (folder.isDirectory()) {
        File propsFile = new File(folder, HelperMisc.PROVIDER_PROPERTIES_FILE);
        if (propsFile.exists() && !propsFile.isDirectory()) {
          AppDetails app = this.readAppDescriptor(folder.getName(), propsFile);
          apps.add(app);
        }
      }
    }

    // Compare with the defs list!
    // Are there any differences?
    for (AppDetails singleApp : apps) {
      final Long id = super.list(singleApp.getName());
      AppDetails previousAppDetails = this.get(id);

      // It didn't exist...
      if (previousAppDetails == null) {
        LOGGER.log(Level.INFO,
            "New app found! Adding new app: {0}", singleApp.getName().getValue());

        // Either is the first time running or it is a newly installed app!
        ObjectId source = null;
        this.add(singleApp, source, providerURI);
        anyChanges = true;
        continue; // Check the next one...
      }

      // It did exist before. Are there any differences from the previous?
      if (!previousAppDetails.equals(singleApp)) {
        // Is it a difference just in the Running status?
        if (AppsLauncherManager.isJustRunningStatusChange(previousAppDetails, singleApp)) {
          continue;
        }

        // Then we have to update it...
        LOGGER.log(Level.INFO,
            "New update found on app: {0}\nPrevious: {1}\nNew: {2}",
            new Object[]{singleApp.getName().getValue(),
              previousAppDetails.toString(), singleApp.toString()});

        this.update(id, singleApp, null);
        anyChanges = true;
      }
    }

    // Also needs to check if we removed a folder!
    final LongList ids = this.listAll();
    final AppDetailsList localApps = this.getAll();
    for (int i = 0; i < ids.size(); i++) { // Roll all the apps inside the apps folder
      AppDetails localApp = localApps.get(i);
      boolean appStillIntact = false;
      for (File folder : fList) { // Roll all the apps inside the apps folder
        if (folder.isDirectory()) {
          if (folder.getName().equals(localApp.getName().getValue())) {
            for (File file : folder.listFiles()) { // Roll all the files inside each app folder
              // Check if the folder contains the provider properties
              if (HelperMisc.PROVIDER_PROPERTIES_FILE.equals(file.getName())) {
                // All Good!
                appStillIntact = true;
                break;
              }
            }
          }
        }
      }

      if (!appStillIntact) {
        LOGGER.log(Level.INFO,
            "The app has been removed: {0}", localApp.getName().getValue());

        this.delete(ids.get(i));
        anyChanges = true;
      }
    }

    return anyChanges;
  }

  protected boolean isAppRunning(final Long appId)
  {
    // get it from the list of available apps
    AppDetails app = (AppDetails) this.getDef(appId);
    ProcessExecutionHandler handler = handlers.get(appId);

    if (handler == null) {
      LOGGER.log(Level.FINE,
          "The Process handler could not be found!");

      app.setRunning(false);
      return false;
    }

    return this.get(appId).getRunning();
  }

  protected String[] assembleAppStopCommand(final String appName)
  {
    ArrayList<String> ret = new ArrayList<>();
    String trimmedAppName = appName.replaceAll("space-app-", "");
    if (osValidator.isWindows()) {
      ret.add("cmd");
      ret.add("/c");
      ret.add("stop_" + trimmedAppName + ".bat");
    } else {
      ret.add("/bin/sh");
      ret.add("stop_" + trimmedAppName + ".sh");
    }
    return ret.toArray(new String[ret.size()]);
  }

  protected String[] assembleAppLauncherCommand(final String appName, String runAs)
  {
    ArrayList<String> ret = new ArrayList<>();
    String trimmedAppName = appName.replaceAll("space-app-", "");
    if (osValidator.isWindows()) {
      ret.add("cmd");
      ret.add("/c");
      ret.add("start_" + trimmedAppName + ".bat");
    } else {
      String quote = "";
      if (runAs != null) {
        ret.add("su");
        ret.add("-");
        ret.add(runAs);
        ret.add("-m");
        ret.add("-c");
        quote = "'";
      } else {
        ret.add("/bin/sh");
      }

      ret.add(quote + "./start_" + trimmedAppName + ".sh" + quote); // Ugly workaround but works!
    }
    return ret.toArray(new String[ret.size()]);
  }

  protected void assembleAppLauncherEnvironment(final String directoryServiceURI,
      final Map<String, String> targetEnv)
  {
    try {
      targetEnv.putAll(EnvironmentUtils.getProcEnvironment());
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "getProcEnvironment failed!", ex);
    }
    // Extend the current environment by JAVA_OPTS
    targetEnv.put("JAVA_OPTS",
        "-D" + Const.CENTRAL_DIRECTORY_URI_PROPERTY + "=" + directoryServiceURI + "");
    // Ensure spawned apps do not inherit eventual library path of the Supervisor
    targetEnv.remove("LD_LIBRARY_PATH");
  }

  protected void startAppProcess(final ProcessExecutionHandler handler,
      final MALInteraction interaction, final String directoryServiceURI) throws IOException
  {
    // get it from the list of available apps
    AppDetails app = (AppDetails) this.getDef(handler.getObjId());

    // Go to the folder where the app are installed
    final File appFolder
        = new File(appsFolderPath + File.separator + app.getName().getValue());
    final String[] appLauncherCommand = assembleAppLauncherCommand(app.getName().getValue(),
        app.getRunAs());

    final ProcessBuilder pb = new ProcessBuilder(appLauncherCommand);
    Map<String, String> env = pb.environment();
    env.clear();
    assembleAppLauncherEnvironment(directoryServiceURI, env);
    pb.directory(appFolder);
    LOGGER.log(Level.INFO,
        "Initializing ''{0}'' app in dir: {1}, using launcher command: {2}, and env: {3}",
        new Object[]{app.getName().getValue(), appFolder.getAbsolutePath(), Arrays.toString(
          appLauncherCommand), Arrays.toString(EnvironmentUtils.toStrings(env))});
    final Process proc = pb.start();
    handler.monitorProcess(proc);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> proc.destroy()));
    handlers.put(handler.getObjId(), handler);
    this.setRunning(handler.getObjId(), true, interaction); // Update the Archive
  }

  protected boolean killAppProcess(final Long appInstId, MALInteraction interaction) throws
      MALException, MALInteractionException
  {
    AppDetails app = (AppDetails) this.getDef(appInstId); // get it from the list of available apps

    ProcessExecutionHandler handler = handlers.get(appInstId);

    if (handler == null) {
      app.setRunning(false);
      return false;
    }

    if (handler.getProcess() == null) {
      app.setRunning(false);
      return true;
    }

    handler.close();
    this.setRunning(handler.getObjId(), false, interaction); // Update the Archive
    handlers.remove(appInstId); // Get rid of it!

    return true;
  }

  protected boolean stopNativeApp(final Long appInstId, StopAppInteraction interaction) throws
      IOException, MALInteractionException, MALException
  {
    ProcessExecutionHandler handler = handlers.get(appInstId);
    AppDetails app = (AppDetails) this.getDef(appInstId); // get it from the list of available apps

    // Go to the folder where the app are installed
    final File appFolder
        = new File(appsFolderPath + File.separator + app.getName().getValue());
    final String[] appLauncherCommand = assembleAppStopCommand(app.getName().getValue());

    final ProcessBuilder pb = new ProcessBuilder(appLauncherCommand);
    Map<String, String> env = pb.environment();
    env.clear();
    assembleAppLauncherEnvironment("", env);
    pb.directory(appFolder);
    LOGGER.log(Level.INFO,
        "Stopping ''{0}'' app in dir: {1}, using launcher command: {2}, and env: {3}",
        new Object[]{app.getName().getValue(), appFolder.getAbsolutePath(), Arrays.toString(
          appLauncherCommand), Arrays.toString(EnvironmentUtils.toStrings(env))});
    final Process proc = pb.start();
    interaction.sendUpdate(appInstId);
    boolean exitCleanly = false;
    try {
      exitCleanly = proc.waitFor(APP_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      LOGGER.log(Level.WARNING, null, ex);
    }
    if (!exitCleanly) {
      LOGGER.log(Level.WARNING,
          "App {0} stop script did not exit within the timeout ({1} ms). Killing the stop script and forcing the app exit.",
          new Object[]{app.getName().getValue(), APP_STOP_TIMEOUT});
      proc.destroy();
    }

    if (handler == null) {
      app.setRunning(false);
      return false;
    }

    if (handler.getProcess() == null) {
      app.setRunning(false);
      return true;
    }

    handler.close();
    this.setRunning(handler.getObjId(), false, interaction.getInteraction()); // Update the Archive
    handlers.remove(appInstId); // Get rid of it!
    return true;
  }

  protected void stopNMFApp(final Long appInstId, final Identifier appDirectoryServiceName,
      final SingleConnectionDetails appConnection,
      final StopAppInteraction interaction) throws MALException, MALInteractionException
  {
    ClosingAppListener listener = null;
    // Register on the Event service of the respective apps
    // Select all object numbers from the Apps Launcher service Events
    Subscription eventSub = HelperCOM.generateSubscriptionCOMEvent(
        "ClosingAppEvents",
        AppsLauncherHelper.APP_OBJECT_TYPE);
    try { // Subscribe to events
      EventConsumerServiceImpl eventServiceConsumer = new EventConsumerServiceImpl(appConnection);
      Logger.getLogger(AppsLauncherProviderServiceImpl.class.getName()).log(Level.FINE,
          "Connected to: {0}", appConnection.toString());
      listener = new ClosingAppListener(interaction, eventServiceConsumer,
          appInstId);
      eventServiceConsumer.addEventReceivedListener(eventSub, listener);
    } catch (MalformedURLException ex) {
      Logger.getLogger(AppsLauncherProviderServiceImpl.class.getName()).log(Level.SEVERE,
          "Could not connect to the app!");
    }

    // Stop the app...
    ObjectType objType = AppsLauncherHelper.STOPAPP_OBJECT_TYPE;
    Logger.getLogger(AppsLauncherProviderServiceImpl.class.getName()).log(Level.INFO,
        "Sending event to app: {0} (Name: ''{1}'')", new Object[]{appInstId, appDirectoryServiceName});
    this.setRunning(appInstId, false, interaction.getInteraction());
    ObjectId eventSource
        = super.getCOMServices().getActivityTrackingService().storeCOMOperationActivity(
            interaction.getInteraction(), null);

    // Generate, store and publish the events to stop the App...
    final Long objId = super.getCOMServices().getEventService().generateAndStoreEvent(objType,
        ConfigurationProviderSingleton.getDomain(), null, appInstId, eventSource, interaction.
        getInteraction());

    final URI uri = interaction.getInteraction().getMessageHeader().getURIFrom();

    try {
      IdentifierList eventBodies = new IdentifierList(1);
      eventBodies.add(appDirectoryServiceName);
      super.getCOMServices().getEventService().publishEvent(uri, objId, objType, appInstId,
          eventSource, eventBodies);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }

    if (listener != null) {
      try {
        listener.waitForAppClosing(APP_STOP_TIMEOUT);
      } catch (InterruptedException ex) {
        LOGGER.log(Level.WARNING, null, ex);
      }
    }
  }

  /**
   * Stops multiple apps.
   * Blocks until all applications exit or waiting for them times out.
   *
   * @param appInstIds Applications IDs
   * @param appDirectoryServiceNames Directory service app name
   * @param appConnections Application connection handlers (for NMF apps only)
   * @param interaction Source interaction
   * @throws MALException
   * @throws MALInteractionException
   */
  protected void stopApps(final LongList appInstIds, final IdentifierList appDirectoryServiceNames,
      final ArrayList<SingleConnectionDetails> appConnections,
      final StopAppInteraction interaction) throws MALException, MALInteractionException
  {
    boolean[] isNmf = new boolean[appInstIds.size()];
    for (int i = 0; i < isNmf.length; i++) {
      isNmf[i] = this.get(appInstIds.get(i)).getCategory().getValue().equals("NMF_App");
    }

    for (int i = 0; i < appInstIds.size(); i++) {
      long appInstId = appInstIds.get(i);
      AppDetails curr = this.get(appInstId);
      String fileExt = ".sh";
      if (osValidator.isWindows()) {
        fileExt = ".bat";
      }
      File stopScript = new File(appsFolderPath + File.separator + curr.getName().getValue()
          + File.separator + "stop_" + curr.getName().getValue() + fileExt);
      boolean stopExists = stopScript.exists();
      if (isNmf[i]) {
        if (stopExists) {
          final String[] appLauncherCommand = assembleAppStopCommand(curr.getName().getValue());
          final File appFolder
              = new File(appsFolderPath + File.separator + curr.getName().getValue());
          final ProcessBuilder pb = new ProcessBuilder(appLauncherCommand);
          Map<String, String> env = pb.environment();
          env.clear();
          assembleAppLauncherEnvironment("", env);
          pb.directory(appFolder);
          LOGGER.log(Level.INFO,
              "Stopping native component of ''{0}'' app in dir: {1}, using launcher command: {2}, and env: {3}",
              new Object[]{curr.getName().getValue(), appFolder.getAbsolutePath(), Arrays.toString(
                appLauncherCommand), Arrays.toString(EnvironmentUtils.toStrings(env))});
          try {
            final Process proc = pb.start();
          } catch (IOException ex) {
            Logger.getLogger(AppsLauncherManager.class.getName()).log(Level.SEVERE,
                "Stopping native component failed", ex);
          }
        }
        this.stopNMFApp(appInstId, appDirectoryServiceNames.get(i), appConnections.get(i), interaction);
      } else {
        if (!stopExists) {
          LOGGER.log(Level.INFO, "No stop script present for app {0}. Killing the process.",
              curr.getName());
          this.killAppProcess(appInstIds.get(i), interaction.getInteraction());
        } else {
          try {
            LOGGER.log(Level.INFO, "Stop script present for app {0}. Invoking it.", curr.getName());
            this.stopNativeApp(appInstIds.get(i), interaction);
          } catch (IOException ex) {
            Logger.getLogger(AppsLauncherManager.class.getName()).log(Level.SEVERE,
                "Stopping native app failed", ex);
          }
        }
      }
    }
  }

  public void setRunning(Long appInstId, boolean running, MALInteraction interaction)
  {
    this.get(appInstId).setRunning(running);
    this.update(appInstId, this.get(appInstId), interaction); // Update the Archive
  }

  public static SingleConnectionDetails getSingleConnectionDetailsFromProviderSummaryList(
      ProviderSummaryList providersList) throws IOException
  {
    if (providersList.isEmpty()) { // Throw error!
      LOGGER.log(Level.WARNING,
          "The service could not be found in the Directory service... Possible reasons:\n"
          + "1. Wrong area number.\n"
          + "2. User is trying to control a non-NMF app! If so, one needs to use killApp operation!\n");
      throw new IOException();
    }

    if (providersList.size() != 1) { // Throw error!
      throw new IOException("There are more than 1 provider registered for this app! "
          + "Most likely the app was forcefully killed before.");
    }

    // Get the service address details lists
    ServiceCapabilityList capabilities = providersList.get(0).getProviderDetails().
        getServiceCapabilities();

    // How many addresses do we have?
    if (capabilities.isEmpty()) { // Throw an error
      throw new IOException("We don't have any services...");
    }

    if (capabilities.size() != 1) {
      throw new IOException("We have more than 1 service...");
    }

    final AddressDetailsList addresses = capabilities.get(0).getServiceAddresses();

    int bestIndex = HelperCommon.getBestIPCServiceAddressIndex(addresses);
    SingleConnectionDetails connectionDetails = new SingleConnectionDetails();
    connectionDetails.setProviderURI(addresses.get(bestIndex).getServiceURI());
    connectionDetails.setBrokerURI(addresses.get(bestIndex).getBrokerURI());
    connectionDetails.setDomain(providersList.get(0).getProviderKey().getDomain());
    return connectionDetails;
  }

  private static boolean isJustRunningStatusChange(final AppDetails previousAppDetails,
      final AppDetails single_app)
  {
    if (!previousAppDetails.getCategory().equals(single_app.getCategory())
        || !previousAppDetails.getDescription().equals(single_app.getDescription())
        || !previousAppDetails.getName().equals(single_app.getName())
        || previousAppDetails.getRunAtStartup().booleanValue() != single_app.getRunAtStartup().
        booleanValue()
        || !previousAppDetails.getVersion().equals(single_app.getVersion())) {
      return false;
    }

    // extraInfo field can be null according to the API
    if (previousAppDetails.getExtraInfo() != null && single_app.getExtraInfo() != null) {
      if (!previousAppDetails.getExtraInfo().equals(single_app.getExtraInfo())) {
        return false;
      }
    }

    return previousAppDetails.getRunning().booleanValue() != single_app.getRunning().booleanValue();
  }

  private AppDetails readAppDescriptor(final String appName, final File propertiesFile)
  {
    final AppDetails app = new AppDetails();
    app.setName(new Identifier(appName)); // Use the name of the folder

    try {
      final Properties props = new Properties();
      final FileInputStream inputStream = new FileInputStream(propertiesFile);
      props.load(inputStream);
      inputStream.close();
      app.setExtraInfo(HelperMisc.PROVIDER_PROPERTIES_FILE);

      final String category = (props.getProperty(HelperMisc.APP_CATEGORY) != null) ? props.
          getProperty(HelperMisc.APP_CATEGORY) : "-";
      final String version = (props.getProperty(HelperMisc.APP_VERSION) != null) ? props.
          getProperty(HelperMisc.APP_VERSION) : "-";
      final String copyright = (props.getProperty(HelperMisc.APP_COPYRIGHT) != null) ? props.
          getProperty(HelperMisc.APP_COPYRIGHT) : "-";
      final String description = (props.getProperty(HelperMisc.APP_DESCRIPTION) != null) ? props.
          getProperty(HelperMisc.APP_DESCRIPTION) : "-";
      final String user = (props.getProperty(HelperMisc.APP_USER) != null) ? props.getProperty(
          HelperMisc.APP_USER)
          : null; // Since the user change is only implemented on linux this dependency is fine

      app.setCategory(new Identifier(category));
      app.setVersion(version);
      app.setCopyright(copyright);
      app.setDescription(description);
      app.setRunAs(user);

      app.setRunAtStartup(false); // This is not supported in this implementation
      app.setRunning(false); // Default values
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }

    return app;
  }

}
