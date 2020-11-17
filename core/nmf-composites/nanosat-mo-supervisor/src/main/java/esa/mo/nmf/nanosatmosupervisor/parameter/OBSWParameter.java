// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the information for an OBSW parameter defined in the datapool XML file.
 *
 * @author Tanguy Soto
 */
public class OBSWParameter {
  /**
   * Parameter ID (object instance id of the ParameterIdentity).
   */
  private long id;

  /**
   * Parameter name (Identifier, body of the ParameterIdentity)
   */
  private String name;

  /**
   * Parameter description (description field of the ParameterDefinitionDetails)
   */
  private String description;

  /**
   * Parameter type (rawType field of the ParameterDefinitionDetails)
   */
  private String type;

  /**
   * Parameter unit (rawUnit field of the ParameterDefinitionDetails)
   */
  private String unit;

  /**
   * The OBSW aggregations that include this parameter.
   */
  private List<OBSWAggregation> aggregations = new ArrayList<OBSWAggregation>();


  /**
   * Returns the type.
   *
   * @return The type.
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type The type.
   */
  public void setType(String type) {
    this.type = type;
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
   * @param description The description.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the unit.
   *
   * @return The unit.
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Sets the unit.
   *
   * @param unit The unit.
   */
  public void setUnit(String unit) {
    this.unit = unit;
  }

  /**
   * Returns the OBSW aggregations that include this parameter.
   * 
   * @return A list of the aggregations
   */
  public List<OBSWAggregation> getAggregations() {
    return aggregations;
  }

  /**
   * Add an OBSW aggregations to the list of aggregations that include this parameter.
   * 
   * @param aggregation The aggregation to add
   */
  public void addAggregation(OBSWAggregation aggregation) {
    aggregations.add(aggregation);
  }

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
   * @param id The ID.
   */
  public void setId(long id) {
    this.id = id;
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
   * @param name The name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return String.format("OBSWParameter[id=%s, name=%s, description=%s, type=%s, unit=%s]", id,
        name, description, type, unit);
  }
}
