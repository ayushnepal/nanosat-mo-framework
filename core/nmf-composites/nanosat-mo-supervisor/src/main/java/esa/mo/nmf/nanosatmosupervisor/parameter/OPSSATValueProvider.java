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
 * Class actually talking to the OPS-SAT OBSW to fetch real values of the OBSW parameters. Fetched
 * values are placed in a cache to avoid overloading the OBSW.
 *
 * @author Tanguy Soto
 */
public class OPSSATValueProvider implements IOBSWParameterValuesProvider {

  /**
   * The logger
   */
  private static final Logger LOGGER = Logger.getLogger(OPSSATValueProvider.class.getName());

  /**
   * Object providing the list of OBSW parameters
   */
  private final ParameterLister parameterLister;

  /**
   * Object handling caching of the values
   */
  private final CacheHandler cacheHandler;

  /**
   * Creates a new instance of OPSSATValueProvider.
   * 
   * @param parameterMap The map of OBSW parameters so they can be accessed by parameter name.
   */
  public OPSSATValueProvider(ParameterLister parameterLister) {
    this.parameterLister = parameterLister;
    this.cacheHandler = new CacheHandler(parameterLister);
    this.cacheHandler.setCachingTime(10 * 1000);
  }

  /**
   * 
   * TODO getNewValue
   *
   * @param identifier
   * @return
   */
  private Attribute getNewValue(Identifier identifier) {
    LOGGER.log(Level.INFO, "getNewValue(" + identifier + ") called");

    OBSWParameter param = parameterLister.getParameters().get(identifier);
    return HelperAttributes.attributeName2Attribute(param.getType());
  }

  /** {@inheritDoc} */
  @Override
  public Attribute getValue(Identifier identifier) {
    if (cacheHandler.mustRefreshValue(identifier)) {
      Attribute newValue = getNewValue(identifier);
      cacheHandler.cacheValue(newValue, identifier);
      return newValue;
    }
    return cacheHandler.getCachedValue(identifier);
  }
}
