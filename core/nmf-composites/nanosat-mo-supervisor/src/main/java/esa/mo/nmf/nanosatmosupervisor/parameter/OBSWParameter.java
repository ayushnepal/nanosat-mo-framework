// (C) 2020 European Space Agency
// European Space Operations Centre
// Darmstadt, Germany

package esa.mo.nmf.nanosatmosupervisor.parameter;

/**
 * Holds the information for an OBSW parameter defined in the datapool XML file.
 *
 * @author Tanguy Soto
 */
public class OBSWParameter {
  /**
   * Parameter ID (object instance id of the ParameterIdentity).
   */
  private final Long id;

  /**
   * Parameter name (Identifier, body of the ParameterIdentity)
   */
  private final String name;

  /**
   * Parameter description (description field of the ParameterDefinitionDetails)
   */
  private final String description;

  /**
   * Parameter type (rawType field of the ParameterDefinitionDetails)
   */
  private final String type;

  /**
   * Parameter unit (rawUnit field of the ParameterDefinitionDetails)
   */
  private final String unit;

  /**
   * The OBSW aggregation that includes this parameter.
   */
  private OBSWAggregation aggregation;

  /**
   * Creates a new instance of OBSWParameter.
   * 
   * @param id
   * @param name
   * @param description
   * @param type
   * @param unit
   */
  public OBSWParameter(Long id, String name, String description, String type, String unit) {
    super();
    this.id = id;
    this.name = name;
    this.description = description;
    this.type = type;
    this.unit = unit;
  }

  /**
   * Returns the type.
   *
   * @return The type.
   */
  public String getType() {
    return type;
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
   * Returns the unit.
   *
   * @return The unit.
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Returns the OBSW aggregation that includes this parameter.
   * 
   * @return The aggregation
   */
  public OBSWAggregation getAggregation() {
    return aggregation;
  }

  /**
   * Sets the OBSW aggregation that includes this parameter.
   * 
   * @param aggregation The aggregation to add
   */
  public void setAggregation(OBSWAggregation aggregation) {
    this.aggregation = aggregation;
  }

  /**
   * Returns the ID.
   *
   * @return The ID.
   */
  public Long getId() {
    return id;
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
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return String.format("OBSWParameter[id=%s, name=%s, description=%s, type=%s, unit=%s]", id,
        name, description, type, unit);
  }
}
