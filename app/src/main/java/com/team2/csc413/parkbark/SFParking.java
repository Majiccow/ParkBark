package com.team2.csc413.parkbark;

import android.location.Location;
import android.util.JsonReader;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Created by Michael and Aaron on 5/04/15.
 */
public class SFParking {

    // creates a unique instance of SFParking
    public static SFParking service = new SFParking();

    // tag is used for debugging with Log.d(TAG, message)
    private static final String TAG = SFParking.class.getSimpleName();

    // data to be stored from Json request
    private String status,
            message,
            inter,
            tel,
            avail_update_t,
            avail_request_t;
    private int requestID,
        udf1,
        num_records;
    private List<ParkingPlace> parkingList;

    // instantiating default values
    private SFParking() {
        status = null;
        message=null;
        inter=null;
        tel=null;
        avail_request_t=null;
        avail_update_t=null;
        requestID=-1;
        udf1=-1;
        num_records=0;
        parkingList=null;
    }


    public String getStatus() {
        return status;
    }

    public List getParkingList() {
        return parkingList;
    }

    public int getRequestID() {
        return requestID;
    }

    public int getUdf1() {
        return udf1;
    }

    public int getNum_records() {
        return num_records;
    }

    public String getMessage() {
        return message;
    }

    public String getInter() { return inter; }

    public String getTel() { return tel; }

    public String getAvail_update_t() {
        return avail_update_t;
    }

    public String getAvail_request_t() { return avail_request_t; }

    /**
     * returns a list of parking locations
     *
     * @param loc
     * @throws java.io.IOException
     */
    public void retrieveParkingList(Location loc) {
        URL serviceURL = createURL(loc);

        try {
            parkingList = readStreamSFP(serviceURL.openStream());
        }
        catch(IOException io_e) {
            Log.d(TAG, io_e.toString());
        }

    }

    /**
     * returns ArrayList<ParkingPlace> from the Json input stream
     *
     * @param in
     * @return java.util.List
     * @throws IOException
     */
    private List readStreamSFP(InputStream in) throws IOException {

        InputStreamReader isr = new InputStreamReader(in, "UTF-8");

        JsonReader reader = new JsonReader(isr);

        try {
            return readParkingList(reader);
        }
        finally{
            reader.close();
        }
    }

    /**
     * creates a URL object from a location.
     *
     * @param loc
     * @return java.net.URL
     */
    private URL createURL(Location loc) {
        String url_string;
        double lat, lon;

        lat = loc.getLatitude();
        lon = loc.getLongitude();

        url_string = "http://api.sfpark.org/sfpark/rest/availabilityservice?lat="+lat+"&long="+lon
                + "&response=json";

        try {
            URL url = new URL(url_string);
            return url;
        }
        catch (MalformedURLException url_e) {
            Log.d(TAG, url_e.toString());

            return null;
        }
    }

    /**
     * reads information from SF Park, determining if the connection was a success
     * then parses information from SF Park
     *
     * @param reader
     * @return java.util.List
     * @throws IOException
     */
    private List readParkingList(JsonReader reader) throws IOException {
        List parking = new ArrayList();

        reader.beginObject();
        String name = reader.nextName();

        if (name.equals("STATUS")) {

            status = reader.nextString();

            if (status.equals("SUCCESS")) {

                while(reader.hasNext()) {
                    name = reader.nextName();

                    if (name.equals("REQUESTID")) {
                        requestID = reader.nextInt();

                    } else if (name.equals("UDF1")) {
                        udf1 = reader.nextInt();

                    } else if (name.equals("NUM_RECORDS")) {
                        num_records = reader.nextInt();

                    } else if (name.equals("MESSAGE")) {
                        message = reader.nextString();

                    } else if (name.equals("INTER")) {
                        inter = reader.nextString();

                    } else if (name.equals("TEL")) {
                        tel = reader.nextString();

                    } else if (name.equals("AVAILABILITY_UPDATED_TIMESTAMP")) {
                        avail_update_t = reader.nextString();

                    } else if (name.equals("AVAILABILITY_REQUEST_TIMESTAMP")) {
                        avail_request_t = reader.nextString();

                    } else if (name.equals("AVL")) {
                        reader.beginArray();

                        while (reader.hasNext()) {
                            parking.add(readAVL(reader));
                        }

                        reader.endArray();
                    } else {
                        reader.skipValue();
                    }

                }

            } else if (status.equals("ERROR")) {
                status = name;
                Log.d(TAG, reader.nextString());
            } else {
                Log.d(TAG,
        "readParkingList(JsonReader) : \"SUCCESS\" and \"ERROR\" were not found within \"STATUS\"");
            }

        } else {
            Log.d(TAG, "readParkingList(JsonReader) : STATUS not found");
        }

        return parking;
    }

    /**
     * creates a list of parking location from the AVL array within the SF Park Json
     *
     * @param reader
     * @return new Parking Place
     * @throws IOException
     */
    private ParkingPlace readAVL(JsonReader reader) throws IOException {
        String reader_s;

        String type = null,
               name = null,
               desc = null;
        int bfid = -1,
            occ = -1,
            oper = -1,
            pts = -1;
        LocationSFP loc = null;
        List<OPHRS> ophrs = null;
        List<Rate> rates = null;

        reader.beginObject();

        while(reader.hasNext()) {
            reader_s = reader.nextName();

            if (reader_s.equals("TYPE")) {
                type = reader.nextString();

            } else if (reader_s.equals("NAME")) {
                name = reader.nextString();

            } else if (reader_s.equals("DESC")) {
                desc = reader.nextString();

            } else if (reader_s.equals("BFID")) {
                bfid = reader.nextInt();

            } else if (reader_s.equals("OCC")) {
                occ = reader.nextInt();

            } else if (reader_s.equals("OPER")) {
                oper = reader.nextInt();

            } else if (reader_s.equals("PTS")) {
                pts = reader.nextInt();

            } else if (reader_s.equals("LOC")) {
                loc = new LocationSFP( reader.nextString() );

            } else if (reader_s.equals("OPHRS")) {
                ophrs = readOPHRS(reader);

            } else if (reader_s.equals("RATES")) {
                rates = readRates(reader);

            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        return new ParkingPlace(type, name, desc, bfid, occ, oper, pts, ophrs, rates, loc);
    }


    /**
     * returns a list of operating hours for the available parking location
     *
     * @param reader
     * @return ArrayList<OPHRS>
     * @throws IOException
     */
    private List readOPHRS(JsonReader reader) throws IOException {
        List<OPHRS> ophrsList = new ArrayList<OPHRS>();

        String from = null,
               to = null,
               beg = null,
               end = null,
               name;

        reader.beginObject();

        name = reader.nextName();
        if (name.equals("OPS")) {

            reader.beginArray();

            while (reader.hasNext()) {

                reader.beginObject();

                while (reader.hasNext()) {

                    name = reader.nextName();

                    if (name.equals("FROM")) {
                        from = reader.nextString();

                    } else if (name.equals("TO")) {
                        to = reader.nextString();

                    } else if (name.equals("BEG")) {
                        beg = reader.nextString();

                    } else if (name.equals("END")) {
                        end = reader.nextString();

                    } else {
                        reader.skipValue();
                    }
                }

                reader.endObject();

                ophrsList.add(new OPHRS(from, to, beg, end));
            }
            reader.endArray();

        } else {
            Log.d(TAG, "JSON retrieval error: Rate does not contain RS");
        }

        reader.endObject();

        return ophrsList;
    }

    /**
     * returns an list of Rates relating to the available parking location
     *
     * @param reader
     * @return ArrayList<Rate>
     * @throws IOException
     */
    private List readRates(JsonReader reader) throws IOException {
        List<Rate> rates = new ArrayList<Rate>();

        String beg = null,
               end = null,
               rate = null,
               desc = null,
               rq = null,
               rr = null,
               name;

        reader.beginObject();

        name = reader.nextName();

        if (name.equals("RS")) {

            reader.beginArray();

            while (reader.hasNext()) {
                reader.beginObject();

                while (reader.hasNext()) {
                    name = reader.nextName();

                    if (name.equals("BEG")) {
                        beg = reader.nextString();

                    } else if (name.equals("END")) {
                        end = reader.nextString();

                    } else if (name.equals("RATE")) {
                        rate = reader.nextString();

                    } else if (name.equals("DESC")) {
                        desc = reader.nextString();

                    } else if (name.equals("RQ")) {
                        rq = reader.nextString();

                    } else if (name.equals("RR")) {
                        rr = reader.nextString();

                    } else {
                        reader.skipValue();
                    }
                }

                reader.endObject();

                rates.add(new Rate(beg, end, rate, desc, rq, rr));
            }
            reader.endArray();

        } else {
            Log.d(TAG, "JSON retrieval error: Rate does not contain RS");
        }
        reader.endObject();

        return rates;
    }

    /*
     * ParkingPlace helps store data retrieved from SF Park
     */
    public class ParkingPlace {
        private String type,
                       name,
                       desc;
        private int bfid,
                    occ,
                    oper,
                    pts;
        private LocationSFP loc;
        private List<OPHRS> ophrs;
        private List<Rate> rates;

        private ParkingPlace(String type, String name, String desc, int bfid, int occ,
                                  int oper, int pts, List<OPHRS> ophrs, List<Rate> rates, LocationSFP loc ) {
            this.type = type;
            this.name = name;
            this.desc = desc;
            this.bfid = bfid;
            this.occ = occ;
            this.oper = oper;
            this.pts = pts;
            this.ophrs = ophrs;
            this.rates = rates;
            this.loc = loc;

        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }

        public LocationSFP getLoc() {
            return loc;
        }

        public int getBfid() {
            return bfid;
        }

        public int getOcc() {
            return occ;
        }

        public int getOper() {
            return oper;
        }

        public int getPts() {
            return pts;
        }

        public List getOPHRS() {
            return ophrs;
        }

        public List getRates() {
            return rates;
        }
    }

    /*
     * operational hours
     */
    public class OPHRS {
        private String from,
                       to,
                       beg,
                       end;

        protected OPHRS (String from, String to, String beg, String end) {
            this.from = from;
            this.to = to;
            this.beg = beg;
            this.end = end;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getBeg() {
            return beg;
        }

        public String getEnd() {
            return end;
        }
    }

    /*
     * rates
     */
    public class Rate {
        private String beg,
                       end,
                       rate,
                       desc,
                       rateQualifier,
                       rateRestriction;

        protected Rate(String beg, String end, String rate, String desc, String rateQualifier, String rateRestriction) {
            this.beg = beg;
            this.end = end;
            this.rate = rate;
            this.desc = desc;
            this.rateQualifier = rateQualifier;
            this.rateRestriction = rateRestriction;
        }

        public String getBeg() {
            return beg;
        }

        public String getEnd() {
            return end;
        }

        public String getRate() {
            return rate;
        }

        public String getDesc() {
            return desc;
        }

        public String getRateQualifier() {
            return rateQualifier;
        }

        public String getRateRestriction() {
            return rateRestriction;
        }

    }

    public class LocationSFP {
        private int numloc;

        private double lat1, lng1,
               lat2, lng2;

        protected LocationSFP(String location_s) {
            String[] latlng = location_s.split(",");

            if(latlng.length > 2) {
                lng1 = Double.parseDouble(latlng[0]);
                lat1 = Double.parseDouble(latlng[1]);
                lng2 = Double.parseDouble(latlng[2]);
                lat2 = Double.parseDouble(latlng[3]);
                numloc = 2;
            } else {
                lng1 = Double.parseDouble(latlng[0]);
                lat1 = Double.parseDouble(latlng[1]);
                lng2 = -1;
                lat2 = -1;
                numloc = 1;
            }
        }

        public double getLat1() {
            return lat1;
        }
        public double getLng1() {
            return lng1;
        }
        public double getLat2() {
            return lat2;
        }
        public double getLng2() {
            return lng2;
        }
        public int getNumLocations() {
            return numloc;
        }
    }

}
