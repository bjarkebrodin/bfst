package dankmap.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PointOfInterest extends Address implements Serializable {
    private static final long serialVersionUID = -835505199512833278L;
    private final Date timeStamp;

    public PointOfInterest(Address address, Date timeStamp) {
        super(address, address.getAddress());
        this.timeStamp = timeStamp;
    }


    public Date getTimeStamp() {
        return timeStamp;
    }

    public String getFormattedTimeStamp() {
        String pattern = "yyyy-MM-dd HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(timeStamp);
    }

    @Override
    public String toString() {
        return "PointOfInterest{" +
                "address=" + address +
                ", timeStamp=" + timeStamp +
                '}';
    }

}
