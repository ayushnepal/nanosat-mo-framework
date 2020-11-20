// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;

/**
 * Class actually talking to the OPS-SAT OBSW to fetch real values of the OBSW parameters.
 *
 * @author Tanguy Soto
 */
public class OPSSATValueProvider implements IOBSWParameterValuesProvider {

  /** {@inheritDoc} */
  @Override
  public Attribute getValue(Identifier identifier) {
    // TODO Auto-generated method stub
    return null;
  }

}
