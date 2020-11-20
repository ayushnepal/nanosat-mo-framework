// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.util.logging.Level;
import java.util.logging.Logger;
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
  private static final Logger LOGGER = Logger.getLogger(DummyValuesProvider.class.getName());

  /**
   * Object providing the list of OBSW parameters
   */
  private final ParameterLister parameterLister;

  /**
   * Counts how many time a value was queried.
   */
  private static int counter = 0;


  /**
   * Creates a new instance of DummyValuesProvider.
   * 
   * @param parameterMap The map of OBSW parameters so they can be accessed by parameter name.
   */
  public DummyValuesProvider(ParameterLister parameterLister) {
    this.parameterLister = parameterLister;
  }

  /** {@inheritDoc} */
  @Override
  public Attribute getValue(Identifier identifier) {
    counter++;
    LOGGER.log(Level.FINE, "getValue() called for the " + counter + " times");

    OBSWParameter param = parameterLister.getParameters().get(identifier);
    return HelperAttributes.attributeName2Attribute(param.getType());
  }
}
