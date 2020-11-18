// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;

/**
 * Provides the OBSW parameter values through a caching mechanism. Internally it relies on a given
 * values provider to fetch new values when needed.
 *
 * @author Tanguy Soto
 */
public class CacheHandler {

  private static final Logger LOGGER = Logger.getLogger(CacheHandler.class.getName());

  private IOBSWParameterValuesProvider valuesProvider;

  /**
   * Initializes the handler with a given values provider.
   * 
   * @param valuesProvider The values provider
   */
  public CacheHandler(IOBSWParameterValuesProvider valuesProvider) {
    this.valuesProvider = valuesProvider;

    if (this.valuesProvider == null) {
      LOGGER.log(Level.SEVERE,
          "OBSWParameterValuesProvider provided is null, only null parameter values will be returned");
    }
  }

  /**
   * Returns a value for the given parameter name. The value might be new from the values provider
   * or come from the internal cache if eligible.
   *
   * @param identifier Name of the parameter
   * @return The value or null if the parameter name is unknown
   */
  public Attribute getValue(Identifier identifier) {
    if (this.valuesProvider == null) {
      return null;
    }
    // TODO cache values
    return valuesProvider.getValue(identifier);
  }
}
