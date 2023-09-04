package sparkie;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.io.File;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static spark.Spark.*;

public class Main
{
    // Command structure.
    public static String baseCommand = "sudo ./rpi-rgb-led-matrix/utils/led-image-viewer --led-rows=32 --led-cols=64 --led-chain=2  --led-parallel=3 --led-limit-refresh=120 --led-brightness=30 --led-slowdown-gpio=4 -V2 -C --led-scan-mode=1 ";
    public static Process matrixProcess = null;
    
    public static void main (String[] args)
    {
        // Route for posting json payload.
        post("/sparkie", (request, response) -> {
            response.type("text/html");
    
            // Construct payload into JsonObject.
            Gson gson = new Gson();
            JsonObject payload = gson.fromJson(request.body(), JsonObject.class);
            System.out.println(payload.toString());
            if (!payload.has("giphyURL"))
                return "Error";
            
            // Extract gif ID from url.
            String giphyURL = payload.get("giphyURL").getAsString();
            System.out.println("giphyURL: " + giphyURL);
            String giphyID = extractGiphyIDFromURL(giphyURL);
            System.out.println("giphyID: " + giphyID);
            if (giphyID == null)
                return "Error";
            
            // Download file from the world wide web.
            URL downloadURL = new URL(String.format("https://i.giphy.com/media/%s/giphy.gif", giphyID));
            System.out.println("downloadURL: " + downloadURL);
            File downloadedGif = new File("gifs/" + giphyID + ".gif");
            FileUtils.copyURLToFile(downloadURL, downloadedGif);
            
            // Run process to display gif on led matrices.
            if (matrixProcess != null)
            {
    
                for (ProcessHandle processHandle : matrixProcess.children().collect(Collectors.toList()))
                {
                    System.out.println("Found Child to Destroy: " + processHandle.pid());
                    processHandle.destroy();
                }
                
                System.out.println("Destroying");
                matrixProcess.destroy();
                //matrixProcess.children().findFirst().get().destroy();
                System.out.println("Destroyed?");
                
                //return "done";
            }
            String command = baseCommand + downloadedGif.getPath();
            System.out.println("Run This: " + command);
    
            
            /*
            new Thread(() -> {
                Runtime runtime = Runtime.getRuntime();
                try
                {
                    matrixProcess = runtime.exec(command);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }).start();
            */
            
            
            System.out.println("Branching gif display thread.");
            new Thread(() -> {
                    System.out.println("Executing in gif display thread.");
                    ProcessBuilder processBuilder = new ProcessBuilder();
                    processBuilder.command(command.split(" "))
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD);
                System.out.println("Execute finished in gif display thread.");
                try
                {
                    matrixProcess = processBuilder.start();
                    System.out.println("Completed gif display thread with PID: " + matrixProcess.pid());
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }).start();
            System.out.println("Branched gif display thread.");
            
            
            return "Success";
        });
    }
    
    // NEED to break down main method into multiples.
    /* Validate hyperlink
       Check is file exists already (maybe a cache)
       Download file
       display
       checking along the way for any failure points.
     */
    
    private static String extractGiphyIDFromURL (String url)
    {
        /* Need to be able to parse ids out of these formats:
         * https://giphy.com/gifs/13HgwGsXF0aiGY
         * https://giphy.com/gifs/leprechaun-st-paddys-3gNuQeY8FTOgenJHIS
         * https://i.giphy.com/media/3gNuQeY8FTOgenJHIS/giphy.webp
         * https://media.giphy.com/media/13HgwGsXF0aiGY/giphy.gif
         * https://media3.giphy.com/media/3gNuQeY8FTOgenJHIS/giphy.gif
         * https://media4.giphy.com/media/13HgwGsXF0aiGY/giphy.gif?cid=ecf05e47s959yh92425uf9h48614vex3vtpaibyxfap7ai7f&rid=giphy.gif&ct=g
         */
        String gifID = null;
        if (url.contains(".com/gifs/") && url.contains("-"))
        {
            gifID = url.substring(url.lastIndexOf("-") + "-".length());
        }
        else if (url.contains(".com/gifs/"))
        {
            gifID = url.substring(url.lastIndexOf(".com/gifs/") + ".com/gifs/".length());
        }
        if (url.contains(".com/clips/") && url.contains("-"))
        {
            gifID = url.substring(url.lastIndexOf("-") + "-".length());
        }
        else if (url.contains(".com/clips/"))
        {
            gifID = url.substring(url.lastIndexOf(".com/clips/") + ".com/clips/".length());
        }
        else if (url.contains(".com/media/"))
        {
            gifID = url.substring(url.lastIndexOf(".com/media/") + ".com/media/".length(), url.indexOf("/", url.lastIndexOf(".com/media/") + ".com/media/".length()));
        }
        
        return gifID;
    }
}
