// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;

/**
 * Interface that a class providing values for OBSW parameters has to implement.
 *
 * @author Tanguy Soto
 */
public interface IOBSWParameterValuesProvider {

  /**
   * Returns a value for the given parameter name.
   *
   * @param identifier Name of the parameter
   * @return The value or null if the parameter name is unknown
   */
  public Attribute getValue(Identifier identifier);

}
