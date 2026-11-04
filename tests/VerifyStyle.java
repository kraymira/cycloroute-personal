import java.io.*;
import java.nio.file.*;
import java.util.*;
import net.osmand.render.*;
import net.osmand.binary.*;

public class VerifyStyle {
 static Path dir;
 static String roadStyle="";
 static boolean overlays=false;
 static Map<String,String> constants=new HashMap<>();
 static RenderingRulesStorage load(String name) throws Exception {
  RenderingRulesStorage r=new RenderingRulesStorage(name,constants);
  r.parseRulesFromXmlInputStream(Files.newInputStream(dir.resolve(name+".render.xml")),
   (n,ref)-> { try{return load(n);}catch(Exception e){throw new IOException(e);}},false);
  return r;
 }
 static RenderingRuleSearchRequest query(RenderingRulesStorage s,String tag,String val,int zoom,boolean night,String... additional) {
  BinaryMapIndexReader.MapIndex idx=new BinaryMapIndexReader.MapIndex();
  int[] adds=new int[additional.length];int i=1;
  for(String a:additional){String[] p=a.split("=",2);idx.initMapEncodingRule(1,i,p[0],p[1]);adds[i-1]=i;i++;}
  BinaryMapDataObject obj=new BinaryMapDataObject(1,new int[]{1,1,2,2},null,2,false,new int[0],adds,0,0);obj.setMapIndex(idx);
  RenderingRuleSearchRequest r=new RenderingRuleSearchRequest(s);
  r.setInitialTagValueZoom(tag,val,zoom,obj);
  for(RenderingRuleProperty p:s.PROPS.getCustomRules()){
   if(p.isBoolean())r.setBooleanFilter(p,false);
   else if(p.isString())r.setStringFilter(p,"");
  }
  r.setStringFilter(s.PROPS.get("baseAppMode"),"bicycle");
  r.setStringFilter(s.PROPS.get("roadStyle"),roadStyle);
  r.setBooleanFilter(s.PROPS.get("showSurfaces"),overlays);
  r.setBooleanFilter(s.PROPS.get("showSurfaceGrade"),overlays);
  r.setBooleanFilter(s.PROPS.get("showAccess"),true);
  r.setBooleanFilter(s.PROPS.R_NIGHT_MODE,night);
  r.setBooleanFilter(s.PROPS.get("engine_v1"),false);
  r.search(RenderingRulesStorage.LINE_RULES);
  return r;
 }
 static String color(RenderingRulesStorage s,RenderingRuleSearchRequest r,String prop){return String.format("#%06X",r.getIntPropertyValue(s.PROPS.get(prop))&0xffffff);}
 static boolean cold(int c){float[] h=java.awt.Color.RGBtoHSB((c>>16)&255,(c>>8)&255,c&255,null);return h[0]*360>=155&&h[0]*360<=280&&h[1]>.07;}
 static void expect(String actual,String expected,String label){if(!actual.equals(expected))throw new AssertionError(label+" expected "+expected+" got "+actual);}
 public static void main(String[]args)throws Exception{
  dir=Path.of(args[0]);
  for(String n:new String[]{"CycloRoutePersonalBase","CycloRoutePersonal"}){
   var doc=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(dir.resolve(n+".render.xml").toFile());
   var cs=doc.getElementsByTagName("renderingConstant");
   for(int i=0;i<cs.getLength();i++){var e=(org.w3c.dom.Element)cs.item(i);constants.put(e.getAttribute("name"),e.getAttribute("value"));}
  }
  RenderingRulesStorage s=load("CycloRoutePersonal");int count=0;
  for(String style:new String[]{"Osmand","germanRoadAtlas","americanRoadAtlas","highContrastRoads","boldOutline"}){
   roadStyle=style;
   for(boolean night:new boolean[]{false,true})for(int z:new int[]{13,15,17,19}){
    for(String value:new String[]{"motorway","trunk","primary","secondary","tertiary","residential","unclassified","service","busway","pedestrian","road","track","path","footway","bridleway"}){
     for(boolean ov:new boolean[]{false,true}){
      overlays=ov;var r=query(s,"highway",value,z,night,"surface=concrete","smoothness=excellent");
      for(String prop:new String[]{"color","color_2","color_3","color_4","color_5"}){
       var p=s.PROPS.get(prop);if(r.isSpecified(p)&&cold(r.getIntPropertyValue(p)))throw new AssertionError("Cold road "+style+" "+value+" "+prop+" "+color(s,r,prop));
      }count++;
     }
    }
   }
  }
  overlays=false;roadStyle="";
  // Guard the user's visibility requirement: bike lines must remain bold at
  // overview zooms, with a casing wider than the coloured centre.
  for(boolean night:new boolean[]{false,true})for(int z:new int[]{10,11,12,13,14,15,16,17,18,19}){
   float width=z<=11?3:z==12?4:z==13?5:z<=15?6:z==16?7:z==17?8:9;
   var bike=query(s,"highway","cycleway",z,night);
   expect(color(s,bike,"color_5"),"#174BCC","overview cycleway colour");
   if(bike.getFloatPropertyValue(s.PROPS.get("strokeWidth_5"))!=width)throw new AssertionError("Bike width at z"+z);
   if(bike.getFloatPropertyValue(s.PROPS.get("strokeWidth_4"))!=width+2)throw new AssertionError("Bike casing at z"+z);
   count+=3;
   for(String value:new String[]{"lane","track","shared_lane","share_busway"}){
    var q=query(s,"cycleway",value,z,night);
    if(q.getFloatPropertyValue(s.PROPS.get("strokeWidth"))!=width)throw new AssertionError("Attached width "+value+" z"+z);
    if(q.getFloatPropertyValue(s.PROPS.get("strokeWidth_0"))!=width+2)throw new AssertionError("Attached casing "+value+" z"+z);
    count+=2;
   }
   for(String value:new String[]{"path","footway"}){
    var q=query(s,"highway",value,z,night,"bicycle=designated");
    if(q.getFloatPropertyValue(s.PROPS.get("strokeWidth_5"))!=width)throw new AssertionError("Mixed width "+value+" z"+z);
    count++;
   }
  }
  expect(color(s,query(s,"highway","cycleway",17,false,"bicycle=no"),"color_5"),"#D32F2F","cycleway prohibition");count++;
  for(boolean night:new boolean[]{false,true})for(int z:new int[]{13,14,15,16,17,18,19}){
   expect(color(s,query(s,"highway","cycleway",z,night),"color_5"),"#174BCC","cycleway z"+z);count++;
   expect(color(s,query(s,"highway","path",z,night,"bicycle=designated","foot=designated"),"color_5"),"#4A9FD9","shared path");count++;
   expect(color(s,query(s,"highway","cycleway",z,night,"foot=designated"),"color_5"),"#4A9FD9","shared cycleway");count++;
   for(String[] c:new String[][]{{"lane","#6C91EF"},{"track","#174BCC"},{"shared_lane","#7DC8D4"},{"share_busway","#7DC8D4"}}){
    expect(color(s,query(s,"cycleway",c[0],z,night),"color"),c[1],c[0]);count++;
   }
   expect(color(s,query(s,"highway","residential",z,night,"bicycle=no"),"color_5"),"#D32F2F","prohibition");count++;
   expect(color(s,query(s,"highway","residential",z,night,"bicycle_road=yes"),"color_5"),"#7DB1D4","bicycle street");count++;
   expect(color(s,query(s,"highway","residential",z,night,"cyclestreet=yes"),"color_5"),"#7DB1D4","cyclestreet");count++;
   for(String segregated:new String[]{"yes","no"}){
    var q=query(s,"highway","path",z,night,"bicycle=designated","foot=designated","segregated="+segregated);
    expect(color(s,q,"color_5"),"#4A9FD9","same mixed colour");
    expect(q.getStringPropertyValue(s.PROPS.get("pathEffect_5")),"","same solid mixed line");count++;
   }
   for(String value:new String[]{"motorway","trunk","primary","secondary","tertiary","residential","unclassified","service","busway","pedestrian","road","track","path","footway","bridleway"}){
    var r=query(s,"highway",value,z,night);
    for(String prop:new String[]{"color","color_2","color_3","color_4","color_5"}){
     var p=s.PROPS.get(prop);if(r.isSpecified(p)&&cold(r.getIntPropertyValue(p)))throw new AssertionError("Cold non-bike highway "+value+" "+prop+" "+color(s,r,prop));
    }count++;
   }
  }
  System.out.println("PASS "+count+" rendering-rule colour and width checks, parsed with OsmAnd RenderingRulesStorage");
  File f=dir.getParent().resolve("CycloRoutePersonalLegend.obf").toFile();
  var reader=new BinaryMapIndexReader(new RandomAccessFile(f,"r"),f);
  try{
   if(reader.containsRouteData()||reader.getMapIndexes().isEmpty())throw new AssertionError("Legend must contain map data only");
   System.out.println("PASS: legend OBF readable, map section present, no routing section");
  }finally{reader.close();}
 }
}
