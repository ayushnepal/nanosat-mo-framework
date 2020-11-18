// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.util.Map;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;
import esa.mo.helpertools.helpers.HelperAttributes;

/**
 * Just a dummy parameter value provider that return Attributes with default values of either 0 or
 * null depending on the actual type.
 *
 * @author Tanguy Soto
 */
public class DummyValuesProvider implements IOBSWParameterValuesProvider {

  /**
   * The map of OBSW parameters so they can be accessed by parameter name.
   */
  private final Map<String, OBSWParameter> parameterMap;

  /**
   * Creates a new instance of DummyValuesProvider.
   * 
   * @param parameterMap The map of OBSW parameters so they can be accessed by parameter name.
   */
  public DummyValuesProvider(Map<String, OBSWParameter> parameterMap) {
    this.parameterMap = parameterMap;
  }

  /** {@inheritDoc} */
  @Override
  public Attribute getValue(Identifier identifier) {
    OBSWParameter param = parameterMap.get(identifier.getValue());
    return HelperAttributes.attributeName2Attribute(param.getType());
  }
}
