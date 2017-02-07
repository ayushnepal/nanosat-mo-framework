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
package esa.mo.common.impl.provider;

import esa.mo.com.impl.util.COMServicesProvider;
import esa.mo.com.impl.util.HelperArchive;
import esa.mo.com.impl.util.HelperCOM;
import esa.mo.helpertools.connections.ConnectionProvider;
import esa.mo.helpertools.connections.ServicesConnectionDetails;
import esa.mo.helpertools.connections.SingleConnectionDetails;
import esa.mo.helpertools.helpers.HelperMisc;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.com.COMHelper;
import org.ccsds.moims.mo.com.archive.structures.ArchiveDetailsList;
import org.ccsds.moims.mo.com.structures.ObjectKey;
import org.ccsds.moims.mo.common.CommonHelper;
import org.ccsds.moims.mo.common.directory.DirectoryHelper;
import org.ccsds.moims.mo.common.directory.body.PublishProviderResponse;
import org.ccsds.moims.mo.common.directory.provider.DirectoryInheritanceSkeleton;
import org.ccsds.moims.mo.common.directory.structures.AddressDetails;
import org.ccsds.moims.mo.common.directory.structures.AddressDetailsList;
import org.ccsds.moims.mo.common.directory.structures.ProviderDetails;
import org.ccsds.moims.mo.common.directory.structures.ProviderDetailsList;
import org.ccsds.moims.mo.common.directory.structures.ProviderSummary;
import org.ccsds.moims.mo.common.directory.structures.ProviderSummaryList;
import org.ccsds.moims.mo.common.directory.structures.PublishDetails;
import org.ccsds.moims.mo.common.directory.structures.ServiceCapability;
import org.ccsds.moims.mo.common.directory.structures.ServiceCapabilityList;
import org.ccsds.moims.mo.common.directory.structures.ServiceFilter;
import org.ccsds.moims.mo.common.structures.ServiceKey;
import org.ccsds.moims.mo.mal.MALContextFactory;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALHelper;
import org.ccsds.moims.mo.mal.MALInteractionException;
import org.ccsds.moims.mo.mal.MALStandardError;
import org.ccsds.moims.mo.mal.provider.MALInteraction;
import org.ccsds.moims.mo.mal.provider.MALProvider;
import org.ccsds.moims.mo.mal.structures.FileList;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.LongList;
import org.ccsds.moims.mo.mal.structures.NamedValue;
import org.ccsds.moims.mo.mal.structures.NamedValueList;
import org.ccsds.moims.mo.mal.structures.QoSLevel;
import org.ccsds.moims.mo.mal.structures.QoSLevelList;
import org.ccsds.moims.mo.mal.structures.UInteger;
import org.ccsds.moims.mo.mal.structures.UOctet;
import org.ccsds.moims.mo.mal.structures.UShort;

/**
 *
 */
public class DirectoryProviderServiceImpl extends DirectoryInheritanceSkeleton {

    private static final String PROPERTY_NAME_SERVICE = "name";
    private MALProvider directoryServiceProvider;
    private boolean initialiased = false;
    private boolean running = false;
    private final ConnectionProvider connection = new ConnectionProvider();
    private final HashMap<Long, PublishDetails> providersAvailable = new HashMap<Long, PublishDetails>();
    private final Object MUTEX = new Object();
    private COMServicesProvider comServices;

    /**
     * creates the MAL objects, the publisher used to create updates and starts
     * the publishing thread
     *
     * @param comServices
     * @throws MALException On initialisation error.
     */
    public synchronized void init(COMServicesProvider comServices) throws MALException {
        if (!initialiased) {
            if (MALContextFactory.lookupArea(MALHelper.MAL_AREA_NAME, MALHelper.MAL_AREA_VERSION) == null) {
                MALHelper.init(MALContextFactory.getElementFactoryRegistry());
            }

            if (MALContextFactory.lookupArea(COMHelper.COM_AREA_NAME, COMHelper.COM_AREA_VERSION) == null) {
                COMHelper.deepInit(MALContextFactory.getElementFactoryRegistry());
            }

            if (MALContextFactory.lookupArea(CommonHelper.COMMON_AREA_NAME, CommonHelper.COMMON_AREA_VERSION) == null) {
                CommonHelper.init(MALContextFactory.getElementFactoryRegistry());
            }

            try {
                DirectoryHelper.init(MALContextFactory.getElementFactoryRegistry());
            } catch (MALException ex) { // nothing to be done..
            }
        }

        this.comServices = comServices;

        // shut down old service transport
        if (null != directoryServiceProvider) {
            connection.closeAll();
        }

        directoryServiceProvider = connection.startService(DirectoryHelper.DIRECTORY_SERVICE_NAME.toString(), DirectoryHelper.DIRECTORY_SERVICE, false, this);

        running = true;
        initialiased = true;
        Logger.getLogger(DirectoryProviderServiceImpl.class.getName()).info("Directory service READY");

    }

    /**
     * Closes all running threads and releases the MAL resources.
     */
    public void close() {
        try {
            if (null != directoryServiceProvider) {
                directoryServiceProvider.close();
            }

            connection.closeAll();
            running = false;
        } catch (MALException ex) {
            Logger.getLogger(DirectoryProviderServiceImpl.class.getName()).log(Level.WARNING, "Exception during close down of the provider {0}", ex);
        }
    }

    public ConnectionProvider getConnection() {
        return this.connection;
    }

    @Override
    public ProviderSummaryList lookupProvider(ServiceFilter filter, MALInteraction interaction) throws MALInteractionException, MALException {

        if (null == filter) { // Is the input null?
            throw new IllegalArgumentException("filter argument must not be null");
        }

        IdentifierList inputDomain = filter.getDomain();

        // Check if the domain contains any wildcard that is not in the end, if so, throw error
        for (int i = 0; i < inputDomain.size(); i++) {
            Identifier domainPart = inputDomain.get(i);

            if (domainPart.toString().equals("*") && i != (inputDomain.size() - 1)) {
                throw new MALInteractionException(new MALStandardError(COMHelper.INVALID_ERROR_NUMBER, null));
            }
        }

        final HashMap<Long, PublishDetails> list = new HashMap<Long, PublishDetails>();

        synchronized (MUTEX) {
            list.putAll(providersAvailable);
        }

        LongList keys = new LongList();
        keys.addAll(list.keySet());

        // Initialize the final Provider Summary List
        ProviderSummaryList outputList = new ProviderSummaryList();

        // Filter...
        for (int i = 0; i < keys.size(); i++) { // Filter through all providers

            PublishDetails provider = list.get(keys.get(i));
            ProviderSummary providerOutput = new ProviderSummary();

            //Check service provider name
            if (!filter.getServiceProviderName().toString().equals("*")) { // If not a wildcard...
                if (!provider.getProviderName().toString().equals(filter.getServiceProviderName().toString())) {
                    continue;
                }
            }

            if (HelperCOM.domainContainsWildcard(filter.getDomain())) {  // Does it contain a wildcard in the filter?
                // Compare each object one by one...

                if (!HelperCOM.domainMatchesWildcardDomain(provider.getDomain(), inputDomain)) {
                    continue;
                }

            } else // Direct match...
            {
                if (!inputDomain.equals(provider.getDomain())) {
                    continue;
                }
            }

            // Check session type
            if (filter.getSessionType() != null) {
                if (!provider.getSessionType().equals(filter.getSessionType())) {
                    continue;
                }
            }

            // Check session name
            if (!filter.getSessionName().toString().equals("*")) {
                if (!provider.getSourceSessionName().toString().equals(filter.getSessionName().toString())) {
                    continue;
                }
            }

            // Set the Provider Details structure
            ProviderDetails outProvDetails = new ProviderDetails();
            outProvDetails.setProviderAddresses(provider.getProviderDetails().getProviderAddresses());

            ServiceCapabilityList outCap = new ServiceCapabilityList();

            // Check each service
            for (int j = 0; j < provider.getProviderDetails().getServiceCapabilities().size(); j++) { // Go through all the services

                ServiceCapability serviceCapability = provider.getProviderDetails().getServiceCapabilities().get(j);
//                AddressDetails providerAddress = provider.getProviderDetails().getProviderAddresses().get(j);

                // Check service key - area field
                if (filter.getServiceKey().getArea().getValue() != 0) {
                    if (!serviceCapability.getServiceKey().getArea().equals(filter.getServiceKey().getArea())) {
                        continue;
                    }
                }

                // Check service key - service field
                if (filter.getServiceKey().getService().getValue() != 0) {
                    if (!serviceCapability.getServiceKey().getService().equals(filter.getServiceKey().getService())) {
                        continue;
                    }
                }

                // Check service key - version field
                if (filter.getServiceKey().getVersion().getValue() != 0) {
                    if (!serviceCapability.getServiceKey().getVersion().equals(filter.getServiceKey().getVersion())) {
                        continue;
                    }
                }

                // Check service capabilities
                if (!filter.getRequiredCapabilities().isEmpty()) { // Not empty...
                    boolean capExists = false;

                    for (UInteger cap : filter.getRequiredCapabilities()) {
                        // cycle all the ones available in the provider
                        for (UInteger proCap : filter.getRequiredCapabilities()) {
                            if (cap.equals(proCap)) {
                                capExists = true;
                            }
                        }
                    }

                    if (!capExists) { // If the capability we want does not exist, then get out...
                        continue;
                    }
                }

                // Add the service to the list of matching services
                outCap.add(serviceCapability);
            }

            // It passed all the tests!
            final ObjectKey objKey = new ObjectKey(provider.getDomain(), keys.get(i));
            providerOutput.setProviderKey(objKey);
            providerOutput.setProviderName(provider.getProviderName());

            outProvDetails.setServiceCapabilities(outCap);
            providerOutput.setProviderDetails(outProvDetails);

            outputList.add(providerOutput);
        }

        // Errors
        // The operation does not return any errors.
        return outputList;  // requirement: 3.4.9.2.d

    }

    @Override
    public PublishProviderResponse publishProvider(PublishDetails newProviderDetails, MALInteraction interaction) throws MALInteractionException, MALException {

        Identifier serviceProviderName = newProviderDetails.getProviderName();
        IdentifierList objBodies = new IdentifierList();
        objBodies.add(serviceProviderName);

        ArchiveDetailsList archDetails;

        if (interaction == null) {
            archDetails = HelperArchive.generateArchiveDetailsList(null, null, connection.getConnectionDetails());
        } else {
            archDetails = HelperArchive.generateArchiveDetailsList(null, null, interaction);
        }

        // Check if there are comServices...
        if (comServices == null) {
            throw new MALInteractionException(new MALStandardError(COMHelper.INVALID_ERROR_NUMBER, null));
        }

        // Check if the archive is available...
        if (comServices.getArchiveService() == null) {
            throw new MALInteractionException(new MALStandardError(COMHelper.INVALID_ERROR_NUMBER, null));
        }

        // Store in the Archive the ServiceProvider COM object and get an object instance identifier
        LongList returnedServProvObjIds = comServices.getArchiveService().store(
                true,
                DirectoryHelper.SERVICEPROVIDER_OBJECT_TYPE,
                connection.getConnectionDetails().getDomain(),
                archDetails,
                objBodies,
                null
        );

        Long servProvObjId;

        if (!returnedServProvObjIds.isEmpty()) {
            servProvObjId = returnedServProvObjIds.get(0);
        } else {  // Nothing was returned...
            throw new MALInteractionException(new MALStandardError(COMHelper.INVALID_ERROR_NUMBER, null));
        }

        // related contains the objId of the ServiceProvider object
        ArchiveDetailsList archDetails1 = (interaction == null)
                ? HelperArchive.generateArchiveDetailsList(servProvObjId, null, connection.getConnectionDetails())
                : HelperArchive.generateArchiveDetailsList(servProvObjId, null, interaction);

        ProviderDetailsList capabilities = new ProviderDetailsList();
        capabilities.add(newProviderDetails.getProviderDetails());

        // Store in the Archive the ProviderCapabilities COM object and get an object instance identifier
        comServices.getArchiveService().store(
                false,
                DirectoryHelper.PROVIDERCAPABILITIES_OBJECT_TYPE,
                connection.getConnectionDetails().getDomain(),
                archDetails1,
                capabilities,
                null
        );

        synchronized (MUTEX) {
            this.providersAvailable.put(servProvObjId, newProviderDetails);
        }

        PublishProviderResponse response = new PublishProviderResponse();
        response.setBodyElement0(servProvObjId);
        response.setBodyElement1(null); // All capabilities (does null really mean that?)

        return response;
    }

    @Override
    public void withdrawProvider(Long providerObjectKey, MALInteraction interaction) throws MALInteractionException, MALException {
        synchronized (MUTEX) {
            PublishDetails details = this.providersAvailable.get(providerObjectKey);

            if (details == null) { // The requested provider does not exist
                throw new MALInteractionException(new MALStandardError(MALHelper.UNKNOWN_ERROR_NUMBER, null));
            }

            providersAvailable.remove(providerObjectKey); // Remove the provider...
        }
    }

    public PublishDetails autoLoadURIsFile(final String providerName) {
        ServicesConnectionDetails primaryConnectionDetails = new ServicesConnectionDetails();
        ServicesConnectionDetails secondaryAddresses = new ServicesConnectionDetails();

        try {
            primaryConnectionDetails = primaryConnectionDetails.loadURIFromFiles();
        } catch (MalformedURLException ex) {
            Logger.getLogger(DirectoryProviderServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            secondaryAddresses = (System.getProperty(HelperMisc.NMF_SECONDARY_PROTOCOL) != null)
                    ? secondaryAddresses.loadURIFromFiles(HelperMisc.PROVIDER_URIS_SECONDARY_PROPERTIES_FILENAME)
                    : null;
        } catch (MalformedURLException ex) {
            Logger.getLogger(DirectoryProviderServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

        HashMap<String, SingleConnectionDetails> connsMap = primaryConnectionDetails.getServices();
        Object[] keys = connsMap.keySet().toArray();

        final ServiceCapabilityList capabilities = new ServiceCapabilityList();

        // Iterate all the services and make them available...
        for (int i = 0; i < keys.length; i++) {
            String serviceName = (String) keys[i];
            SingleConnectionDetails conn = connsMap.get(serviceName);
            AddressDetails serviceAddress = DirectoryProviderServiceImpl.getServiceAddressDetails(conn);

            AddressDetailsList serviceAddresses = new AddressDetailsList();
            serviceAddresses.add(serviceAddress);

            ServiceKey key = new ServiceKey();
            key.setArea(new UShort(conn.getServiceKey().get(0)));
            key.setService(new UShort(conn.getServiceKey().get(1)));
            key.setVersion(new UOctet(conn.getServiceKey().get(2).shortValue()));

            ServiceCapability capability = new ServiceCapability();
            capability.setServiceKey(key);
            capability.setSupportedCapabilities(null); // "If NULL then all capabilities supported."
            NamedValueList serviceProps = new NamedValueList();
            serviceProps.add(new NamedValue(new Identifier(PROPERTY_NAME_SERVICE), new Identifier(serviceName)));
            capability.setServiceProperties(serviceProps);
            capability.setServiceAddresses(serviceAddresses);

            capabilities.add(capability);
        }

        // Second iteration needed here for the secondaryAddresses
        if (secondaryAddresses != null) {
            connsMap = secondaryAddresses.getServices();
            keys = connsMap.keySet().toArray();

            for (int i = 0; i < keys.length; i++) {
                String serviceName = (String) keys[i];
                SingleConnectionDetails conn = connsMap.get(serviceName);
                AddressDetails serviceAddress = DirectoryProviderServiceImpl.getServiceAddressDetails(conn);

                AddressDetailsList serviceAddresses = this.findAddressDetailsListOfService(serviceName, capabilities);

                if (serviceAddresses == null) { // If not found
                    serviceAddresses = new AddressDetailsList();

                    // Then create a new capability object
                    ServiceKey key = new ServiceKey();
                    key.setArea(new UShort(conn.getServiceKey().get(0)));
                    key.setService(new UShort(conn.getServiceKey().get(1)));
                    key.setVersion(new UOctet(conn.getServiceKey().get(2).shortValue()));

                    ServiceCapability capability = new ServiceCapability();
                    capability.setServiceKey(key);
                    capability.setSupportedCapabilities(null); // "If NULL then all capabilities supported."
                    NamedValueList serviceProps = new NamedValueList();
                    serviceProps.add(new NamedValue(new Identifier(PROPERTY_NAME_SERVICE), new Identifier(serviceName)));
                    capability.setServiceProperties(serviceProps);
                    capability.setServiceAddresses(serviceAddresses);

                    capabilities.add(capability);
                }

                serviceAddresses.add(serviceAddress);
            }
        }

        ProviderDetails serviceDetails = new ProviderDetails();
        serviceDetails.setServiceCapabilities(capabilities);
        serviceDetails.setProviderAddresses(new AddressDetailsList());

        PublishDetails newProviderDetails = new PublishDetails();
        newProviderDetails.setProviderName(new Identifier(providerName));
        newProviderDetails.setDomain(connection.getConnectionDetails().getDomain());
        newProviderDetails.setSessionType(connection.getConnectionDetails().getConfiguration().getSession());
        newProviderDetails.setSourceSessionName(null);
        newProviderDetails.setNetwork(connection.getConnectionDetails().getConfiguration().getNetwork());
        newProviderDetails.setProviderDetails(serviceDetails);

        try {
            this.publishProvider(newProviderDetails, null);
            return newProviderDetails;
        } catch (MALInteractionException ex) {
            Logger.getLogger(DirectoryProviderServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MALException ex) {
            Logger.getLogger(DirectoryProviderServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    private static AddressDetails getServiceAddressDetails(SingleConnectionDetails conn) {
        QoSLevelList qos = new QoSLevelList();
        qos.add(QoSLevel.ASSURED);
        NamedValueList qosProperties = new NamedValueList();  // Nothing here for now...

        AddressDetails serviceAddress = new AddressDetails();
        serviceAddress.setSupportedLevels(qos);
        serviceAddress.setQoSproperties(qosProperties);
        serviceAddress.setPriorityLevels(new UInteger(1));  // hum?
        serviceAddress.setServiceURI(conn.getProviderURI());
        serviceAddress.setBrokerURI(conn.getBrokerURI());
        serviceAddress.setBrokerProviderObjInstId(null);

        return serviceAddress;
    }

    private AddressDetailsList findAddressDetailsListOfService(String serviceName, ServiceCapabilityList capabilities) {
        if (serviceName == null) {
            return null;
        }

        // Iterate all capabilities until you find the serviceName
        for (ServiceCapability capability : capabilities) {
            if (capability != null) {
                for (NamedValue serviceProp : capability.getServiceProperties()) {
/*                    
                    if (PROPERTY_NAME_SERVICE.equals(serviceProp.getName().getValue())
                            && serviceName.equals(((Identifier) serviceProp.getValue()).getValue())) {
                        return capability.getServiceAddresses();
                    }
*/                    
                    if (PROPERTY_NAME_SERVICE.equals(serviceProp.getName().getValue())
                            && serviceName.equals(((Identifier) serviceProp.getValue()).getValue())) {
                        return capability.getServiceAddresses();
                    }
                    

                }
            }
        }

        return null; // Not found!
    }

    @Override
    public FileList getServiceXML(Long l, MALInteraction mali) throws MALInteractionException, MALException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}