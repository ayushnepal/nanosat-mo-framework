// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

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


  public OBSWParameterManager(InputStream datapool, InputStream aggregations)
      throws ParserConfigurationException, SAXException, IOException {
    parameterReader = new ParameterReader(datapool);
    aggregationReader = new AggregationReader(aggregations, parameterReader.getParameters());
  }

  /**
   * Returns the list of parameters defined in the OBSW.
   *
   * @return The list of parameters
   */
  public List<OBSWParameter> getParameters() {
    return new ArrayList<>(parameterReader.getParameters().values());
  }
}
