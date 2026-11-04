import java.io.*;
import java.nio.file.*;
import java.util.*;
import btools.router.*;

/** Desktop/reference adapter. This does not install a service in OsmAnd. */
public class BoundedRoute {
 public record Result(OsmTrack track, RoutingContext context) {}
 public static Result route(File segments,File profiles,String profile,double[] coords) {
  var points=new ArrayList<OsmNodeNamed>();
  for(int i=0;i<4;i+=2){var n=new OsmNodeNamed();n.name=i==0?"A":"B";n.ilon=(int)Math.round((coords[i]+180)*1e6);n.ilat=(int)Math.round((coords[i+1]+90)*1e6);points.add(n);}
  var rc=new RoutingContext();rc.localFunction=new File(profiles,profile).getAbsolutePath();
  var engine=new RoutingEngine(null,null,segments,points,rc);engine.doRun(120000);
  if(engine.getErrorMessage()!=null)throw new IllegalStateException(profile+": "+engine.getErrorMessage());
  return new Result(engine.getFoundTrack(),rc);
 }
 public static boolean withinLimit(int preferred,int baseline){return (long)preferred*100 <= (long)baseline*120;}
 public static boolean sameEndpoints(OsmTrack a,OsmTrack b){
  for(int i:new int[]{0,1}){
   var x=a.nodes.get(i==0?0:a.nodes.size()-1);var y=b.nodes.get(i==0?0:b.nodes.size()-1);
   if(x.getILon()!=y.getILon()||x.getILat()!=y.getILat())return false;
  }return true;
 }
 public static void main(String[]args)throws Exception{
  if(args.length!=7)throw new IllegalArgumentException("segmentsDir profilesDir fromLon fromLat toLon toLat outputPrefix");
  double[] coords=new double[4];for(int i=0;i<4;i++)coords[i]=Double.parseDouble(args[i+2]);
  var baseline=route(new File(args[0]),new File(args[1]),"cycleways-baseline.brf",coords);
  Result preferred=null;String status="preferred-unavailable";
  try{preferred=route(new File(args[0]),new File(args[1]),"cycleways-preferred.brf",coords);status="over-limit-or-different-snap";}catch(IllegalStateException e){System.err.println(e.getMessage());}
  var selected=baseline;
  if(preferred!=null && sameEndpoints(preferred.track,baseline.track) && withinLimit(preferred.track.distance,baseline.track.distance)){selected=preferred;status="preferred";}
  Files.writeString(Path.of(args[6]+".gpx"),new FormatGpx(selected.context).format(selected.track));
  Files.writeString(Path.of(args[6]+".geojson"),new FormatJson(selected.context).format(selected.track));
  String report=String.format(Locale.ROOT,"{\"baseline_m\":%d,\"preferred_m\":%s,\"selected_m\":%d,\"limit_percent\":20,\"status\":\"%s\"}%n",baseline.track.distance,preferred==null?"null":Integer.toString(preferred.track.distance),selected.track.distance,status);
  Files.writeString(Path.of(args[6]+".json"),report);System.out.print(report);
 }
}
