/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
 * ----------------------------------------------------------------------------
 * Licensed under the European Space Agency Public License, Version 2.0
 * You may not use this file except in compliance with the License.
 *
 * Except as expressly set forth in this License, the Software is provided to
 * You on an "as is" basis and without warranties of any kind, including without
 * limitation merchantability, fitness for a particular purpose, absence of
 * defects or errors, accuracy or non-infringement of intellectual property rights.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * ----------------------------------------------------------------------------
 */
package esa.mo.nmf.groundmoadapter;

import esa.mo.helpertools.connections.ConnectionConsumer;
import esa.mo.nmf.commonmoadapter.CommonMOAdapterImpl;
import java.util.logging.Logger;
import org.ccsds.moims.mo.common.directory.structures.ProviderSummary;

/**
 * The implementation of the Ground MO Adapter.
 *
 * @author Cesar Coelho
 */
public class GroundMOAdapterImpl extends CommonMOAdapterImpl
{

  /* Logger */
  private static final Logger LOGGER = Logger.getLogger(GroundMOAdapterImpl.class.getName());

  /**
   * The constructor of this class
   *
   * @param connection The connection details of the provider
   */
  public GroundMOAdapterImpl(final ConnectionConsumer connection)
  {
    super(connection);
    super.init();
  }

  /**
   * The constructor of this class
   *
   * @param providerDetails The Provider details. This object can be obtained from the Directory
   *                        service
   */
  public GroundMOAdapterImpl(final ProviderSummary providerDetails)
  {
    super(providerDetails);
    super.init();
  }
}
