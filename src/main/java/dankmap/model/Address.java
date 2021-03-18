package dankmap.model;

import dankmap.util.StringUtil;

import java.io.Serializable;
import java.util.Objects;

public class Address extends Location implements Serializable {
    private static final long serialVersionUID = -868185231256329556L;

    final String address;

    public Address(XYSupplier point, String address) {
        super(point);
        this.address = address.strip().toLowerCase();
    }

    public Address(float x, float y, String address) {
        this(new Location(x, y), address);
    }

    public Address(double x, double y, String address) {
        this(new Location(x, y), address);
    }

    public String getAddress() {
        return address;
    }

    public String getFormattedAddress() {
        return StringUtil.capitalizeAllFirstLetters(address);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;
        if (!super.equals(o)) return false;
        Address address1 = (Address) o;
        return Objects.equals(address, address1.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), address);
    }

    @Override
    public String toString() {
        return "Address{" +
                "address='" + address + '\'' +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
