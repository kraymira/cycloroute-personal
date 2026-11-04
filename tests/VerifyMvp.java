import java.io.*;
import java.util.*;
import btools.expressions.*;
public class VerifyMvp {
 static BExpressionContextWay way;
 static BExpressionContextNode node;
 static int count;
 static void check(float expected,boolean reverse,String...tags){
  int[] data=way.createNewLookupData();
  for(String t:tags){String[]p=t.split("=",2);way.addLookupValue(p[0],p[1],data);}
  way.evaluate(reverse,way.encode(data));
  if(Math.abs(way.getCostfactor()-expected)>.001)throw new AssertionError(Arrays.toString(tags)+" reverse="+reverse+" expected="+expected+" got="+way.getCostfactor());
  count++;
 }
 public static void main(String[]args)throws Exception{
  var meta=new BExpressionMetaData();way=new BExpressionContextWay(meta);node=new BExpressionContextNode(meta);
  meta.readMetaData(new File(args[0]));way.parseFile(new File(args[1]),"global");node.parseFile(new File(args[1]),"global");
  for(boolean rev:new boolean[]{false,true}){
   check(1,rev,"highway=cycleway");
   check(1,rev,"highway=footway","bicycle=designated");
   check(1,rev,"highway=residential","bicycle_road=yes");
   check(1.08f,rev,"highway=residential","cycleway=lane");
   check(1.16f,rev,"highway=residential","cycleway=shared_lane");
   check(1.2f,rev,"highway=residential");
   check(1.2f,rev,"highway=residential","bicycle=yes");
   check(1.2f,rev,"highway=residential","route_bicycle_lcn=yes");
   check(1.2f,rev,"highway=residential","cycleway=separate");
   for(String v:new String[]{"no","private","dismount","use_sidepath"})check(10000,rev,"highway=residential","bicycle="+v);
   for(String h:new String[]{"steps","motorway","construction","footway","path"})check(10000,rev,"highway="+h);
   check(10000,rev,"highway=residential","access=private");
   check(1.2f,rev,"highway=residential","access=no","bicycle=yes");
  }
  check(1,false,"highway=primary","cycleway:right=track");
  check(1.2f,true,"highway=primary","cycleway:right=track");
  check(1.08f,false,"highway=primary","cycleway:right=lane");
  check(1.2f,true,"highway=primary","cycleway:right=lane");
  check(10000,true,"highway=residential","oneway=yes");
  check(1.2f,true,"highway=residential","oneway=yes","oneway:bicycle=no");
  check(10000,false,"highway=residential","bicycle:forward=no");
  for(String tag:new String[]{"bicycle=no","barrier=gate","access=private"}){
   int[] data=node.createNewLookupData();String[]p=tag.split("=");node.addLookupValue(p[0],p[1],data);node.evaluate(false,node.encode(data));
   if(node.getInitialcost()<1000000)throw new AssertionError(tag);count++;
  }
  System.out.println("PASS "+count+" MVP cost, direction, access and barrier checks");
 }
}
