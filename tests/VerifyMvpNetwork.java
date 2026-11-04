import java.io.*;
import btools.router.*;
public class VerifyMvpNetwork {
 static File seg,profiles;static int count;
 static OsmTrack route(String profile,double...coords){return BoundedRoute.route(seg,profiles,profile,coords).track();}
 static void choice(boolean detour,double...coords){
  var a=route("cycleways-baseline.brf",coords);var b=route("cycleways-preferred.brf",coords);
  if(!BoundedRoute.sameEndpoints(a,b))throw new AssertionError("Different endpoints");
  boolean longer=b.distance>a.distance+5;
  if(longer!=detour)throw new AssertionError("expected detour="+detour+" baseline="+a.distance+" preferred="+b.distance);
  if(!BoundedRoute.withinLimit(b.distance,a.distance))throw new AssertionError("Too long");
  System.out.println("MVP detour="+detour+" baseline="+a.distance+"m preferred="+b.distance+"m");count++;
 }
 public static void main(String[]args){
  seg=new File(args[0]);profiles=new File(args[1]);
  var t=route("cycleways-preferred.brf",8.701,50,8.707,50);
  System.out.println("MVP connects broken bicycle network via road: "+t.distance+"m");count++;
  route("cycleways-preferred.brf",8.7035,50,8.707,50);count++;
  choice(true,8.72,50,8.724,50);
  choice(false,8.73,50.01,8.734,50.01);
  if(!BoundedRoute.withinLimit(1200,1000)||BoundedRoute.withinLimit(1201,1000))throw new AssertionError("20% boundary");count++;
  System.out.println("PASS "+count+" MVP full-graph and limit scenarios");
 }
}
