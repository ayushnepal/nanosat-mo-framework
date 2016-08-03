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
package esa.mo.nanosatmoframework.adapters;

import esa.mo.com.impl.provider.ArchivePersistenceObject;
import esa.mo.com.impl.util.COMServicesProvider;
import esa.mo.com.impl.util.HelperArchive;
import esa.mo.helpertools.connections.ConfigurationProvider;
import esa.mo.mc.impl.provider.ActionProviderServiceImpl;
import esa.mo.mc.impl.provider.AggregationProviderServiceImpl;
import esa.mo.mc.impl.provider.AlertProviderServiceImpl;
import esa.mo.mc.impl.provider.CheckProviderServiceImpl;
import esa.mo.mc.impl.provider.ParameterProviderServiceImpl;
import esa.mo.mc.impl.provider.StatisticProviderServiceImpl;
import esa.mo.nanosatmoframework.interfaces.NanoSatMOFrameworkInterface;
import esa.mo.reconfigurable.service.ConfigurationNotificationInterface;
import esa.mo.reconfigurable.service.ReconfigurableServiceImplInterface;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.com.archive.structures.ArchiveDetailsList;
import org.ccsds.moims.mo.com.structures.ObjectId;
import org.ccsds.moims.mo.common.configuration.ConfigurationHelper;
import org.ccsds.moims.mo.common.configuration.structures.ConfigurationObjectDetails;
import org.ccsds.moims.mo.common.configuration.structures.ConfigurationObjectDetailsList;
import org.ccsds.moims.mo.common.configuration.structures.ConfigurationObjectSet;
import org.ccsds.moims.mo.common.configuration.structures.ConfigurationObjectSetList;
import org.ccsds.moims.mo.common.directory.DirectoryHelper;
import org.ccsds.moims.mo.common.directory.structures.ServiceKey;
import org.ccsds.moims.mo.common.directory.structures.ServiceKeyList;
import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.LongList;
import org.ccsds.moims.mo.mal.structures.URI;
import org.ccsds.moims.mo.mal.structures.UShort;
import org.ccsds.moims.mo.mc.MCHelper;
import org.ccsds.moims.mo.mc.action.ActionHelper;
import org.ccsds.moims.mo.mc.aggregation.AggregationHelper;
import org.ccsds.moims.mo.mc.alert.AlertHelper;
import org.ccsds.moims.mo.mc.check.CheckHelper;
import org.ccsds.moims.mo.mc.parameter.ParameterHelper;
import org.ccsds.moims.mo.mc.statistic.StatisticHelper;

/**
 *
 * @author Cesar Coelho
 */
public class MCStoreLastConfigurationAdapter implements ConfigurationNotificationInterface {

    public static Long DEFAULT_OBJID_ACTION_SERVICE = (long) 1;
    public static Long DEFAULT_OBJID_PARAMETER_SERVICE = (long) 2;
    public static Long DEFAULT_OBJID_ALERT_SERVICE = (long) 3;
    public static Long DEFAULT_OBJID_CHECK_SERVICE = (long) 4;
    public static Long DEFAULT_OBJID_STATISTIC_SERVICE = (long) 5;
    public static Long DEFAULT_OBJID_AGGREGATION_SERVICE = (long) 6;

    private ConfigurationProvider configuration = new ConfigurationProvider();
    private final COMServicesProvider comServices;

    public MCStoreLastConfigurationAdapter(NanoSatMOFrameworkInterface provider, ObjectId confId, Identifier providerName) {

        try {
            ConfigurationHelper.init(MALContextFactory.getElementFactoryRegistry());
        } catch (MALException ex) {
            // if it was already initialized, then.. cool bro!
        }

        try {
            DirectoryHelper.init(MALContextFactory.getElementFactoryRegistry());
        } catch (MALException ex) {
            // if it was already initialized, then.. cool bro!
        }

        this.comServices = provider.getCOMServices();

        ArchivePersistenceObject comObject = HelperArchive.getArchiveCOMObject(
                comServices.getArchiveService(),
                ConfigurationHelper.PROVIDERCONFIGURATION_OBJECT_TYPE,
                confId.getKey().getDomain(),
                confId.getKey().getInstId());

        // Does the providerConfiguration object exists?
        if (comObject != null) {
            return;
        }

        Logger.getLogger(MCStoreLastConfigurationAdapter.class.getName()).log(Level.INFO, "There were no previous configurations stored in the Archive. Creating configurations...");

        // It doesn't exist... create all the necessary objects...
        try {
            // Store the Default Service Configuration objects
            Long objIdAction = this.storeDefaultServiceConfiguration(DEFAULT_OBJID_ACTION_SERVICE,
                    ActionHelper.ACTION_SERVICE_NUMBER, provider.getMCServices().getActionService());

            Long objIdParameter = this.storeDefaultServiceConfiguration(DEFAULT_OBJID_PARAMETER_SERVICE,
                    ParameterHelper.PARAMETER_SERVICE_NUMBER, provider.getMCServices().getParameterService());

            Long objIdAlert = this.storeDefaultServiceConfiguration(DEFAULT_OBJID_ALERT_SERVICE,
                    AlertHelper.ALERT_SERVICE_NUMBER, provider.getMCServices().getAlertService());
            /*
            Long objIdCheck = this.storeDefaultServiceConfiguration(DEFAULT_OBJID_CHECK_SERVICE,
                    CheckHelper.CHECK_SERVICE_NUMBER, provider.getMCServices().getCheckService());

            Long objIdStatistic = this.storeDefaultServiceConfiguration(DEFAULT_OBJID_STATISTIC_SERVICE,
                    StatisticHelper.STATISTIC_SERVICE_NUMBER, provider.getMCServices().getStatisticService());
             */
            Long objIdAggregation = this.storeDefaultServiceConfiguration(DEFAULT_OBJID_AGGREGATION_SERVICE,
                    AggregationHelper.AGGREGATION_SERVICE_NUMBER, provider.getMCServices().getAggregationService());

            // Store the provider configuration objects
            ConfigurationObjectDetailsList archObj = new ConfigurationObjectDetailsList();
            ConfigurationObjectDetails providerObjects = new ConfigurationObjectDetails();
            ConfigurationObjectSetList setList = new ConfigurationObjectSetList();
            ConfigurationObjectSet set = new ConfigurationObjectSet();

            LongList objIds = new LongList();
            objIds.add(objIdAction);
            objIds.add(objIdParameter);
            objIds.add(objIdAlert);
//            objIds.add(objIdCheck);
//            objIds.add(objIdStatistic);
            objIds.add(objIdAggregation);

            set.setDomain(configuration.getDomain());
            set.setObjType(ConfigurationHelper.SERVICECONFIGURATION_OBJECT_TYPE);
            set.setObjInstIds(objIds);

            setList.add(set);
            providerObjects.setConfigObjects(setList);
            archObj.add(providerObjects);

            LongList objIds3 = comServices.getArchiveService().store(
                    true,
                    ConfigurationHelper.CONFIGURATIONOBJECTS_OBJECT_TYPE,
                    configuration.getDomain(),
                    HelperArchive.generateArchiveDetailsList(null, null, configuration.getNetwork(), new URI("")),
                    archObj,
                    null);

            // Store the provider configuration
            // Related points to the Provider's Configuration Objects
            ArchiveDetailsList details = HelperArchive.generateArchiveDetailsList(objIds3.get(0), null, configuration.getNetwork(), new URI(""));
            details.get(0).setInstId(confId.getKey().getInstId());
            IdentifierList providerNameList = new IdentifierList();
            providerNameList.add(providerName);

            comServices.getArchiveService().store(
                    false,
                    ConfigurationHelper.PROVIDERCONFIGURATION_OBJECT_TYPE,
                    configuration.getDomain(),
                    details,
                    providerNameList,
                    null);

        } catch (MALException ex) {
            Logger.getLogger(MCStoreLastConfigurationAdapter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MALInteractionException ex) {
            Logger.getLogger(MCStoreLastConfigurationAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }

        // If not, then create it and all of the configuration objects for each service
    }

    @Override
    public void configurationChanged(ReconfigurableServiceImplInterface serviceImpl) {
        // Is the service implementation an instance of...
        if (serviceImpl instanceof ActionProviderServiceImpl) {
            this.updateConfigurationInArchive(serviceImpl, DEFAULT_OBJID_ACTION_SERVICE);
        }

        if (serviceImpl instanceof ParameterProviderServiceImpl) {
            this.updateConfigurationInArchive(serviceImpl, DEFAULT_OBJID_PARAMETER_SERVICE);
        }

        if (serviceImpl instanceof AlertProviderServiceImpl) {
            this.updateConfigurationInArchive(serviceImpl, DEFAULT_OBJID_ALERT_SERVICE);
        }
        /*
        if (serviceImpl instanceof CheckProviderServiceImpl) {
            this.updateConfigurationInArchive(serviceImpl, DEFAULT_OBJID_CHECK_SERVICE);
        }

        if (serviceImpl instanceof StatisticProviderServiceImpl) {
            this.updateConfigurationInArchive(serviceImpl, DEFAULT_OBJID_STATISTIC_SERVICE);
        }
         */
        if (serviceImpl instanceof AggregationProviderServiceImpl) {
            this.updateConfigurationInArchive(serviceImpl, DEFAULT_OBJID_AGGREGATION_SERVICE);
        }
    }

    private void updateConfigurationInArchive(final ReconfigurableServiceImplInterface serviceImpl, final Long objId) {

        Thread t1 = new Thread() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();

                // Retrieve the COM object of the service
                ArchivePersistenceObject comObject = HelperArchive.getArchiveCOMObject(comServices.getArchiveService(),
                        ConfigurationHelper.SERVICECONFIGURATION_OBJECT_TYPE, configuration.getDomain(), objId);

                Logger.getLogger(MCStoreLastConfigurationAdapter.class.getName()).log(Level.INFO, "Time 1: " + (System.currentTimeMillis() - startTime));

                // Stuff to feed the update operation from the Archive...
                ArchiveDetailsList details = HelperArchive.generateArchiveDetailsList(null, null, configuration.getNetwork(), new URI(""), comObject.getArchiveDetails().getDetails().getRelated());
                ConfigurationObjectDetailsList confObjsList = new ConfigurationObjectDetailsList();
                confObjsList.add(serviceImpl.getCurrentConfiguration());

                Logger.getLogger(MCStoreLastConfigurationAdapter.class.getName()).log(Level.INFO, "Time 2: " + (System.currentTimeMillis() - startTime));

                try {
                    comServices.getArchiveService().update(
                            ConfigurationHelper.CONFIGURATIONOBJECTS_OBJECT_TYPE,
                            configuration.getDomain(),
                            details,
                            confObjsList,
                            null);
                } catch (MALException ex) {
                    Logger.getLogger(MCStoreLastConfigurationAdapter.class.getName()).log(Level.SEVERE, null, ex);
                } catch (MALInteractionException ex) {
                    Logger.getLogger(MCStoreLastConfigurationAdapter.class.getName()).log(Level.SEVERE, null, ex);
                }

                Logger.getLogger(MCStoreLastConfigurationAdapter.class.getName()).log(Level.INFO, "Duration of the update: " + (System.currentTimeMillis() - startTime));
            }
        };

        t1.start(); // Total time thread: ~1.485 ms

    }

    private Long storeDefaultServiceConfiguration(final Long defaultObjId, final UShort serviceNumber, ReconfigurableServiceImplInterface service) {
        try {
            // Store the Service Configuration objects
            ConfigurationObjectDetailsList archObj1 = new ConfigurationObjectDetailsList();
            archObj1.add(service.getCurrentConfiguration());

            LongList objIds1 = comServices.getArchiveService().store(
                    true,
                    ConfigurationHelper.CONFIGURATIONOBJECTS_OBJECT_TYPE,
                    configuration.getDomain(),
                    HelperArchive.generateArchiveDetailsList(null, null, configuration.getNetwork(), new URI("")),
                    archObj1,
                    null);

            // Store the Service Configuration
            ServiceKeyList serviceKeyList = new ServiceKeyList();
            ServiceKey serviceKey = new ServiceKey(MCHelper.MC_AREA_NUMBER, serviceNumber, MCHelper.MC_AREA_VERSION);
            serviceKeyList.add(serviceKey);

            LongList objIds2 = comServices.getArchiveService().store(
                    true,
                    ConfigurationHelper.SERVICECONFIGURATION_OBJECT_TYPE,
                    configuration.getDomain(),
                    HelperArchive.generateArchiveDetailsList(objIds1.get(0), null, configuration.getNetwork(), new URI(""), defaultObjId),
                    serviceKeyList,
                    null);

            return objIds2.get(0);

        } catch (MALException ex) {
            Logger.getLogger(MCStoreLastConfigurationAdapter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MALInteractionException ex) {
            Logger.getLogger(MCStoreLastConfigurationAdapter.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

}