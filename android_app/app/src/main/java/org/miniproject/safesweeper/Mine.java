package org.miniproject.safesweeper;

public class Mine {

    private double lat;
    private double lng;



    public Mine(double lat, double lng){

        this.lat = lat;
        this.lng = lng;
    }


    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}
