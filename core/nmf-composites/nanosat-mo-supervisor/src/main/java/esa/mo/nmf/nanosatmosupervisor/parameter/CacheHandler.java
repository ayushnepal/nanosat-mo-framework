// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.util.HashMap;
import java.util.Map;
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

  /**
   * The parameters new values provider to call when we have to refresh a parameter value in the
   * cache.
   */
  private final IOBSWParameterValuesProvider valuesProvider;

  private final ParameterLister parameterLister;

  /**
   * Map of OBSW parameter value by parameter name acting as our cache storage.
   */
  private final Map<Identifier, OBSWParameterValue> cache;

  /*
   * Cache configuration settings
   */

  /**
   * Maximum time a parameter value should stay in the cache in milliseconds.
   */
  private final long MAX_CACHING_TIME = 10 * 1000;


  /**
   * Initializes the handler with a given values provider.
   * 
   * @param valuesProvider The values provider
   */
  public CacheHandler(ParameterLister parameterLister,
      IOBSWParameterValuesProvider valuesProvider) {
    this.valuesProvider = valuesProvider;
    this.parameterLister = parameterLister;

    if (this.valuesProvider == null) {
      LOGGER.log(Level.SEVERE,
          "OBSWParameterValuesProvider provided is null, only null parameter values will be returned");
    }
    cache = new HashMap<Identifier, OBSWParameterValue>();
  }

  /**
   * Returns true if the cached value of this parameter has to be refreshed, i.e the values provider
   * has to be called.
   *
   * @param identifier Name of the parameter
   * @return A boolean
   */
  private synchronized boolean mustRefreshValue(Identifier identifier) {
    // Value for this parameter has never been cached
    if (!cache.containsKey(identifier)) {
      return true;
    }

    // This parameter value is outdated
    long now = System.currentTimeMillis();
    if (now - cache.get(identifier).getLastUpdateTime().getTime() > MAX_CACHING_TIME) {
      return true;
    }

    return false;
  }

  /**
   * Returns a new value for the given OBSW parameter and cache it for later retrieval.
   *
   * @param identifier Name of the parameter
   * @return The value or null if the values provider is null
   */
  private Attribute getNewValue(Identifier identifier) {
    if (this.valuesProvider == null) {
      return null;
    }

    Attribute value = valuesProvider.getValue(identifier);
    cacheValue(value, identifier);

    return value;
  }

  /**
   * Returns the cached value for the given OBSW parameter.
   *
   * @param identifier Name of the parameter
   * @return The value or null if the parameter name is unknown
   */
  private synchronized Attribute getCachedValue(Identifier identifier) {
    if (!cache.containsKey(identifier)) {
      return null;
    }
    return cache.get(identifier).getValue();
  }

  /**
   * Caches a value for a given OBSW parameter name
   *
   * @param value Value to cache
   * @param identifier Name of the parameter
   */
  private synchronized void cacheValue(Attribute value, Identifier identifier) {
    if (!cache.containsKey(identifier)) {
      cache.put(identifier,
          new OBSWParameterValue(parameterLister.getParameters().get(identifier), value));
    } else {
      cache.get(identifier).setValue(value);
    }
  }

  /**
   * Returns a value for the given parameter name. The value might be new from the values provider
   * or come from the internal cache if eligible.
   *
   * @param identifier Name of the parameter
   * @return The value
   */
  public Attribute getValue(Identifier identifier) {
    if (mustRefreshValue(identifier)) {
      return getNewValue(identifier);
    }
    return getCachedValue(identifier);
  }
}
