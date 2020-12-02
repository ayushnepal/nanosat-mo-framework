// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.util.HashMap;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;
import esa.mo.helpertools.helpers.HelperAttributes;

/**
 * Provides dummy parameter values. It returns Attribute with default values of either 0 or null
 * depending on the actual type.
 *
 * @author Tanguy Soto
 */
public class DummyValuesProvider extends OBSWParameterValuesProvider {

  /**
   * Creates a new instance of DummyValuesProvider.
   * 
   * @param parameterMap The map of OBSW parameters for which we have to provide values for
   */
  public DummyValuesProvider(HashMap<Identifier, OBSWParameter> parameterMap) {
    super(parameterMap);
  }

  /** {@inheritDoc} */
  @Override
  public Attribute getValue(Identifier identifier) {
    if (!parameterMap.containsKey(identifier)) {
      return null;
    }
    OBSWParameter param = parameterMap.get(identifier);
    return HelperAttributes.attributeName2Attribute(param.getType());
  }
}
