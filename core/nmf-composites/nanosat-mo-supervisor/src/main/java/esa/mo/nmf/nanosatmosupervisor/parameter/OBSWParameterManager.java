// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Duration;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.LongList;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetails;
import org.ccsds.moims.mo.mc.parameter.structures.ParameterDefinitionDetailsList;
import org.ccsds.moims.mo.mc.structures.ObjectInstancePair;
import org.sqlite.SQLiteConfig.Pragma;
import org.xml.sax.SAXException;
import esa.mo.helpertools.helpers.HelperAttributes;
import esa.mo.nmf.MCRegistration;

/**
 * TODO OBSWParameterManager
 *
 * @author Tanguy Soto
 */
public class OBSWParameterManager {
  /**
   * Helper to read the OBSW parameter from datapool.
   */
  private ParameterReader parameterReader;

  /**
   * Helper to read the OBSW aggregations.
   */
  private AggregationReader aggregationReader;

  /**
   * Maps each OBSW parameter id to its proxy id
   */
  private HashMap<Long, Long> paramsIdtoProxiesId;

  public OBSWParameterManager(InputStream datapool, InputStream aggregations)
      throws ParserConfigurationException, SAXException, IOException {
    // Read from provided inputstreams
    parameterReader = new ParameterReader(datapool);
    aggregationReader = new AggregationReader(aggregations, parameterReader.getParameters());

    // Init maps
    paramsIdtoProxiesId = new HashMap<Long, Long>();
  }

  /**
   * Returns the list of parameters defined in the OBSW.
   *
   * @return The list of parameters
   */
  private List<OBSWParameter> getParameters() {
    return new ArrayList<OBSWParameter>(parameterReader.getParameters().values());
  }

  /**
   * Registers proxies for the OBSW parameters using the provided registration object.
   *
   * @param registrationObject The registration object
   */
  public void registerParametersProxies(MCRegistration registrationObject) {
    // Sort parameters by id
    List<OBSWParameter> parameters = getParameters();
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
    LongList proxyIds = registrationObject.registerParameters(paramIdentifiers, paramDefs);

    // Fill in the map of ids
    for (int i = 0; i < proxyIds.size(); i++) {
      paramsIdtoProxiesId.put(parameters.get(i).getId(), proxyIds.get(i));
    }
  }

  /**
   * TODO getValue
   *
   * @param identifier
   * @return
   */
  public Attribute getValue(Identifier identifier) {
    OBSWParameter param = parameterReader.getParameters().get(identifier.getValue());
    return HelperAttributes.attributeName2Attribute(param.getType());
  }
}
