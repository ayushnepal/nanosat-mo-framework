// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.util.HashMap;
import java.util.Map;
import org.ccsds.moims.mo.mal.structures.Attribute;
import org.ccsds.moims.mo.mal.structures.Identifier;

/**
 * Provides the OBSW parameter values through a caching mechanism. Internally it relies on a given
 * values provider to fetch new values when needed.
 *
 * @author Tanguy Soto
 */
public class CacheHandler {

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
  private long cachingTime = 10 * 1000;


  /**
   * Initializes the handler with a given values provider.
   * 
   * @param valuesProvider The values provider
   */
  public CacheHandler(ParameterLister parameterLister) {
    this.parameterLister = parameterLister;

    cache = new HashMap<Identifier, OBSWParameterValue>();
  }

  /**
   * Sets the maximum time a parameter value should stay in the cache in milliseconds.
   * 
   * @param cachingTime the time
   */
  public void setCachingTime(long cachingTime) {
    this.cachingTime = cachingTime;
  }

  /**
   * Returns true if the cached value of this parameter has to be refreshed, i.e the values provider
   * has to be called.
   *
   * @param identifier Name of the parameter
   * @return A boolean
   */
  public synchronized boolean mustRefreshValue(Identifier identifier) {
    // Value for this parameter has never been cached
    if (!cache.containsKey(identifier)) {
      return true;
    }

    long now = System.currentTimeMillis();

    // This parameter value is outdated
    if (now - cache.get(identifier).getLastUpdateTime().getTime() > cachingTime) {
      return true;
    }

    // No need to refresh, cached value is still valid.
    return false;
  }

  /**
   * Returns the cached value for the given OBSW parameter.
   *
   * @param identifier Name of the parameter
   * @return The value or null if the parameter name is unknown (was never cached before)
   */
  public synchronized Attribute getCachedValue(Identifier identifier) {
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
  public synchronized void cacheValue(Attribute value, Identifier identifier) {
    if (!cache.containsKey(identifier)) {
      cache.put(identifier,
          new OBSWParameterValue(parameterLister.getParameters().get(identifier), value));
    } else {
      cache.get(identifier).setValue(value);
    }
  }
}
