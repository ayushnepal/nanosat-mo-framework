// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;

/**
 * Abstract class that a class providing values for OBSW parameters has to extend.
 *
 * @author Tanguy Soto
 */
public abstract class OBSWParameterValuesProvider {
  /**
   * The logger
   */
  private static final Logger LOGGER =
      Logger.getLogger(OBSWParameterValuesProvider.class.getName());

  /**
   * The map of OBSW parameters for which we have to provide values for. The parameters are
   * accessible by their name.
   */
  protected final HashMap<Identifier, OBSWParameter> parameterMap;

  /**
   * Creates a new instance of OBSWParameterValuesProvider.
   * 
   * @param parameterMap The map of OBSW parameters for which we have to provide values for
   */
  public OBSWParameterValuesProvider(HashMap<Identifier, OBSWParameter> parameterMap) {
    if (parameterMap == null) {
      LOGGER.log(Level.SEVERE,
          "Parameters map provided is null, initilazing with empty parameters map");
      parameterMap = new HashMap<Identifier, OBSWParameter>();
    }
    this.parameterMap = parameterMap;
  }

  /**
   * Returns a value for the given parameter name.
   *
   * @param identifier Name of the parameter
   * @return The value or null if the parameter name is unknown or a problem happened while fetching the value
   */
  public abstract Attribute getValue(Identifier identifier);
}
