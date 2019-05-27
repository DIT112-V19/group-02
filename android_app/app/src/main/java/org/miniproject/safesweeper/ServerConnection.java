package org.miniproject.safesweeper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;


public class ServerConnection {

    private ServerInfo files;
    private HttpURLConnection connection;
    private String domain;
    private Gson gson = new Gson();


    public ServerConnection(ServerInfo files) {

        this.files = files;
        this.connection = null;
        this.domain = files.getDomain();

    }

    private HttpURLConnection setConnection(URL url) throws IOException {

        HttpURLConnection connection = null;
        connection = (HttpURLConnection) url.openConnection();

        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        return connection;

    }

    public ArrayList<Mine> getMines() {
        try {
            connection = setConnection(new URL(domain + files.GET_MINES));

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            Type type = new TypeToken<ArrayList<Mine>>() {}.getType();
            ArrayList<Mine> mines = gson.fromJson(in, type);

            return mines;

        } catch (IOException exc) {
            return null;
        }
    }

    public String addMine(double lat, double lng) {
        try {
            System.out.println("lat1" +lat+" lng1 "+lng);
            connection = setConnection(new URL(domain + files.INSERT_MINE));

            StringBuffer sb = new StringBuffer();
            sb.append("lat=");
            sb.append(lat);
            sb.append("&lng=");
            sb.append(lng);
            System.out.println(sb.toString());


            PrintWriter pw = new PrintWriter(connection.getOutputStream(), true);
            pw.print(sb.toString());
            pw.close();
            connection.getInputStream();

            return sb.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }

    }
}
