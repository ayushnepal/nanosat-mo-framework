// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.util.List;

/**
 * Holds the information of an OBSW aggregation defined in the aggregation XML file.
 *
 * @author Tanguy Soto
 */
public class OBSWAggregation {
  /**
   * The aggregation ID (object instance id of the AggregationIdentity).
   */
  private long id;

  /**
   * The aggregation dynamic flag. If true the aggregation definition is already on-board, otherwise
   * it has to be defined.
   */
  private boolean dynamic;

  /**
   * The aggregation built-in flag. If true, this is a special aggregation used to return data from
   * an action.
   */
  private boolean builtin;

  /**
   * The aggregation name (Identifier, body of the AggregationIdentity)
   */
  private String name;

  /**
   * The aggregation category (category field of the AggregationDefinitionDetails)
   */
  private String category;

  /**
   * The aggregation description (description field of the AggregationDefinitionDetails)
   */
  private String description;

  /**
   * The aggregation update interval (reportInterval field of the AggregationDefinitionDetails)
   */
  private long updateInterval;

  /**
   * The aggregation generation enabled flag (generationEnabled field of the
   * AggregationDefinitionDetails)
   */
  private boolean generationEnabled;

  /**
   * The OBSW parameters contained in this aggregation.
   */
  private List<OBSWParameter> parameters;


  /**
   * Returns the ID.
   *
   * @return The ID.
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the ID.
   *
   * @param id The ID to set.
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Returns the dynamic.
   *
   * @return The dynamic.
   */
  public boolean isDynamic() {
    return dynamic;
  }

  /**
   * Sets the dynamic.
   *
   * @param dynamic The dynamic to set.
   */
  public void setDynamic(boolean dynamic) {
    this.dynamic = dynamic;
  }

  /**
   * Returns the builtin.
   *
   * @return The builtin.
   */
  public boolean isBuiltin() {
    return builtin;
  }

  /**
   * Sets the builtin.
   *
   * @param builtin The builtin to set.
   */
  public void setBuiltin(boolean builtin) {
    this.builtin = builtin;
  }

  /**
   * Returns the name.
   *
   * @return The name.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name The name to set.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the category.
   *
   * @return The category.
   */
  public String getCategory() {
    return category;
  }

  /**
   * Sets the category.
   *
   * @param category The category to set.
   */
  public void setCategory(String category) {
    this.category = category;
  }

  /**
   * Returns the description.
   *
   * @return The description.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description The description to set.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the updateInterval.
   *
   * @return The updateInterval.
   */
  public long getUpdateInterval() {
    return updateInterval;
  }

  /**
   * Sets the updateInterval.
   *
   * @param updateInterval The updateInterval to set.
   */
  public void setUpdateInterval(long updateInterval) {
    this.updateInterval = updateInterval;
  }

  /**
   * Returns the generationEnabled.
   *
   * @return The generationEnabled.
   */
  public boolean isGenerationEnabled() {
    return generationEnabled;
  }

  /**
   * Sets the generationEnabled.
   *
   * @param generationEnabled The generationEnabled to set.
   */
  public void setGenerationEnabled(boolean generationEnabled) {
    this.generationEnabled = generationEnabled;
  }

  /**
   * Sets the OBSW parameters of this aggregation.
   *
   * @param parameterList The parameters list to set.
   */
  public void setParameters(List<OBSWParameter> parameterList) {
    this.parameters = parameterList;
  }

  /**
   * Returns the OBSW parameters that this aggregation includes.
   *
   * @return The parameters list.
   */
  public List<OBSWParameter> getParameters() {
    return parameters;
  }

}
