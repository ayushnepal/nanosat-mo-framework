// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
   * Helper to read the OBSW parameter from datapool.
   */
  private final ParameterLister parameterLister;

  /**
   * Helper to read the OBSW aggregations.
   */
  private final AggregationLister aggregationReader;

  /**
   * Provides the OBSW parameter values through a caching mechanism.
   */
  private CacheHandler cacheHandler;

  public OBSWParameterManager(InputStream datapool, InputStream aggregations)
      throws ParserConfigurationException, SAXException, IOException {
    // Read from provided inputstreams
    parameterLister = new ParameterLister(datapool);
    aggregationReader = new AggregationLister(aggregations, parameterLister);

    // Init
    DummyValuesProvider valuesProvider = new DummyValuesProvider(parameterLister);
    cacheHandler = new CacheHandler(parameterLister, valuesProvider);
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
          new Duration(10), null, null));
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
    return cacheHandler.getValue(identifier);
  }
}
