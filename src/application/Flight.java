package application;

public class Flight {
    public String flDate;
    public String mktCarrier;
    public String flightNum;
    public String origin;
    public String originCity;
    public String originState;
    public String originWac;
    public String dest;
    public String destCity;
    public String destState;
    public String destWac;
    public String crsDepTime;
    public String depTime;
    public String crsArrTime;
    public String arrTime;
    public String cancelled;
    public String diverted;
    public String distance;

    public Flight(String[] data) {
        this.flDate = data[0];
        this.mktCarrier = data[1];
        this.flightNum = data[2];
        this.origin = data[3];
        this.originCity = data[4];
        this.originState = data[5];
        this.originWac = data[6];
        this.dest = data[7];
        this.destCity = data[8];
        this.destState = data[9];
        this.destWac = data[10];
        this.crsDepTime = data[11];
        this.depTime = data[12];
        this.crsArrTime = data[13];
        this.arrTime = data[14];
        this.cancelled = data[15];
        this.diverted = data[16];
        this.distance = data[17];
    }
}
