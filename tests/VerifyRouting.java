import java.io.*;
import java.util.*;
import btools.expressions.*;

public class VerifyRouting {
 static BExpressionContextWay way;
 static BExpressionContextNode node;
 static int count;
 static float cost(boolean reverse,String... tags){
  int[] data=way.createNewLookupData();
  for(String s:tags){String[]p=s.split("=",2);way.addLookupValue(p[0],p[1],data);}
  way.evaluate(reverse,way.encode(data));return way.getCostfactor();
 }
 static void test(boolean allowed,boolean rev,String...tags){
  float c=cost(rev,tags);if((c<10000)!=allowed)throw new AssertionError(Arrays.toString(tags)+" reverse="+rev+" cost="+c);count++;
 }
 public static void main(String[]args)throws Exception{
  BExpressionMetaData meta=new BExpressionMetaData();way=new BExpressionContextWay(meta);
  node=new BExpressionContextNode(meta);
  meta.readMetaData(new File(args[0]));way.parseFile(new File(args[1]),"global");node.parseFile(new File(args[1]),"global");
  for(boolean rev:new boolean[]{false,true}){
   test(true,rev,"highway=cycleway");
   test(true,rev,"highway=path","bicycle=designated","foot=designated");
   test(true,rev,"highway=footway","bicycle=designated");
   test(true,rev,"highway=residential","bicycle_road=yes");
   test(true,rev,"highway=residential","cyclestreet=yes");
   test(true,rev,"highway=primary","cycleway=track");
   for(String kind:new String[]{"lane","shared_lane","share_busway","separate","yes"})test(false,rev,"highway=residential","cycleway="+kind);
   test(false,rev,"highway=residential","bicycle=yes");
   test(false,rev,"highway=path","bicycle=yes");
   test(false,rev,"highway=residential","route_bicycle_lcn=yes");
   for(String v:new String[]{"no","private","dismount","destination","use_sidepath","permit","mtb"})test(false,rev,"highway=cycleway","bicycle="+v);
   test(false,rev,"highway=cycleway","access=private");
   test(true,rev,"highway=cycleway","access=no","bicycle=yes");
   test(false,rev,"highway=steps","bicycle=designated");
   test(false,rev,"highway=construction","bicycle_road=yes");
   test(false,rev,"highway=cycleway","route=ferry");
  }
  test(true,false,"highway=primary","cycleway:right=track");
  test(false,true,"highway=primary","cycleway:right=track");
  test(true,true,"highway=primary","cycleway:left=track");
  test(false,false,"highway=primary","cycleway:left=track");
  test(true,true,"highway=primary","cycleway:right=track","cycleway:right:oneway=no");
  test(false,false,"highway=primary","cycleway:right=track","cycleway:right:oneway=-1");
  test(true,true,"highway=primary","cycleway:right=track","cycleway:right:oneway=-1");
  test(false,true,"highway=cycleway","oneway=yes");
  test(true,true,"highway=cycleway","oneway=yes","oneway:bicycle=no");
  test(false,false,"highway=primary","cycleway=track","cycleway:right=lane","cycleway:left=no");
  test(false,false,"highway=cycleway","bicycle:forward=no","bicycle:backward=no");
  test(false,true,"highway=cycleway","bicycle:forward=no","bicycle:backward=no");
  test(false,false,"highway=cycleway","oneway=reversible");
  test(false,true,"highway=cycleway","oneway=reversible");
  for(String[] tags:new String[][]{{"bicycle=no"},{"bicycle=dismount"},{"access=private"},{"barrier=gate"},{"barrier=cycle_barrier"}}){
   int[] data=node.createNewLookupData();for(String t:tags){String[]p=t.split("=",2);node.addLookupValue(p[0],p[1],data);}node.evaluate(false,node.encode(data));
   if(node.getInitialcost()<1000000)throw new AssertionError("Forbidden node "+Arrays.toString(tags));count++;
  }
  System.out.println("PASS "+count+" strict-way admission checks using BRouter expression engine");
 }
}
