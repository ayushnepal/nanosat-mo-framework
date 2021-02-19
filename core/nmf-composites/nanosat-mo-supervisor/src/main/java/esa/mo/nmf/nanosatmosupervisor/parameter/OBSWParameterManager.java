// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetails;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetailsList;
import org.xml.sax.SAXException;
import esa.mo.helpertools.helpers.HelperAttributes;
import esa.mo.nmf.MCRegistration;

/**
 * Handles the provisioning of OBSW parameter.
 *
 * @author Tanguy Soto
 */
public class OBSWParameterManager {
  /**
   * The logger
   */
  private static final Logger LOGGER = Logger.getLogger(OBSWParameterManager.class.getName());

  /**
   * Default OBSW parameter report interval (seconds)
   */
  private static final int DEFAULT_REPORT_INTERVAL = 5;

  /**
   * Helper to read the OBSW parameter from datapool.
   */
  private final ParameterLister parameterLister;

  /**
   * Provides the OBSW parameter values
   */
  private OBSWParameterValuesProvider valuesProvider;

  public OBSWParameterManager(InputStream datapool)
      throws ParserConfigurationException, SAXException, IOException {
    // Read from provided inputstreams
    parameterLister = new ParameterLister(datapool);

    // Instantiate the value provider
    HashMap<Identifier, OBSWParameter> parameterMap = parameterLister.getParameters();
    String valuesProviderClass = System.getProperty("nmf.supervisor.parameter.valuesprovider.impl");
    try {
      Constructor<?> c = Class.forName(valuesProviderClass).getConstructor(parameterMap.getClass());
      valuesProvider = (OBSWParameterValuesProvider) c.newInstance(new Object[] {parameterMap});
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE,
          "Error initializing the values provider. Using dummy values provider.", e);
      valuesProvider = new DummyValuesProvider(parameterMap);
    }
  }

  /**
   * Registers proxies for the OBSW parameters using the provided registration object.
   *
   * @param registrationObject The registration object
   */
  public void registerParametersProxies(MCRegistration registrationObject) {
    // Sort parameters by id
    List<OBSWParameter> parameters =
        new ArrayList<OBSWParameter>(parameterLister.getParameters().values());
    parameters.sort((OBSWParameter p1, OBSWParameter p2) -> p1.getId().compareTo(p2.getId()));

    // Create the parameter proxies definitions
    ParameterDefinitionDetailsList paramDefs = new ParameterDefinitionDetailsList();
    IdentifierList paramIdentifiers = new IdentifierList();

    for (OBSWParameter param : parameters) {
      paramDefs.add(new ParameterDefinitionDetails(param.getDescription(),
          HelperAttributes.attributeName2typeShortForm(param.getType()).byteValue(), "", false,
          new Duration(DEFAULT_REPORT_INTERVAL), null, null));
      paramIdentifiers.add(new Identifier(param.getName()));
    }

    // Register the parameter proxies
    registrationObject.registerParameters(paramIdentifiers, paramDefs);
  }

  /**
   * Returns a value for the given OBSW parameter name.
   *
   * @param identifier Name of the parameter
   * @return The value
   */
  public Attribute getValue(Identifier identifier) {
    return valuesProvider.getValue(identifier);
  }
}
