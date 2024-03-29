package org.atlas.model.adapter.visualparadigm.xml;

public class VpParameter  extends VpTypedElement {
 private String name;
    private String type;
    private String id;
    private String direction;
	private boolean many;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getMany() {
		return many;
	}

	public void setMany(boolean many) {
		this.many = many;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
