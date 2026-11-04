import java.io.*;
import java.nio.file.*;
import java.util.*;
import btools.mapcreator.*;
import btools.router.*;
import org.openstreetmap.osmosis.osmbinary.*;
import com.google.protobuf.ByteString;

public class VerifyNetwork {
 static File work,profiles;
 static void block(DataOutputStream out,String type,byte[] bytes)throws Exception{
  byte[] blob=Fileformat.Blob.newBuilder().setRaw(ByteString.copyFrom(bytes)).build().toByteArray();
  byte[] head=Fileformat.BlobHeader.newBuilder().setType(type).setDatasize(blob.length).build().toByteArray();
  out.writeInt(head.length);out.write(head);out.write(blob);
 }
 static File pbf(File input)throws Exception{
  var doc=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.util.zip.GZIPInputStream(new FileInputStream(input)));
  var st=Osmformat.StringTable.newBuilder();for(String s:new String[]{"","highway","cycleway","residential"})st.addS(ByteString.copyFromUtf8(s));
  var group=Osmformat.PrimitiveGroup.newBuilder();var nodes=doc.getElementsByTagName("node");
  for(int i=0;i<nodes.getLength();i++){var n=(org.w3c.dom.Element)nodes.item(i);group.addNodes(Osmformat.Node.newBuilder().setId(Long.parseLong(n.getAttribute("id"))).setLat(Math.round(Double.parseDouble(n.getAttribute("lat"))*1e7)).setLon(Math.round(Double.parseDouble(n.getAttribute("lon"))*1e7)));}
  var ways=doc.getElementsByTagName("way");
  for(int i=0;i<ways.getLength();i++){var w=(org.w3c.dom.Element)ways.item(i);var way=Osmformat.Way.newBuilder().setId(Long.parseLong(w.getAttribute("id"))).addKeys(1);var tags=w.getElementsByTagName("tag");way.addVals(((org.w3c.dom.Element)tags.item(0)).getAttribute("v").equals("cycleway")?2:3);long prev=0;var refs=w.getElementsByTagName("nd");for(int k=0;k<refs.getLength();k++){long id=Long.parseLong(((org.w3c.dom.Element)refs.item(k)).getAttribute("ref"));way.addRefs(id-prev);prev=id;}group.addWays(way);}
  File result=new File(work,"network.pbf");try(var out=new DataOutputStream(new FileOutputStream(result))){block(out,"OSMHeader",Osmformat.HeaderBlock.newBuilder().addRequiredFeatures("OsmSchema-V0.6").build().toByteArray());block(out,"OSMData",Osmformat.PrimitiveBlock.newBuilder().setStringtable(st).addPrimitivegroup(group).build().toByteArray());}return result;
 }
 static String route(String profile,double flon,double flat,double tlon,double tlat,boolean expected)throws Exception{
  List<OsmNodeNamed> points=new ArrayList<>();
  for(double[] ll:new double[][]{{flon,flat},{tlon,tlat}}){OsmNodeNamed n=new OsmNodeNamed();n.name="p"+points.size();n.ilon=(int)Math.round((ll[0]+180)*1e6);n.ilat=(int)Math.round((ll[1]+90)*1e6);points.add(n);}
  RoutingContext rc=new RoutingContext();rc.localFunction=new File(profiles,profile).getAbsolutePath();
  RoutingEngine e=new RoutingEngine(new File(work,"route").getAbsolutePath(),null,new File(work,"segments"),points,rc);
  e.doRun(10000);String error=e.getErrorMessage();
  if((error==null)!=expected)throw new AssertionError(profile+" expected success="+expected+" got "+error);
  if(expected&&e.getFoundTrack()==null)throw new AssertionError("Missing track");
  return error==null?"route found ("+e.getFoundTrack().distance+"m)":error;
 }
 public static void main(String[]args)throws Exception{
  work=new File(args[0]);work.mkdirs();profiles=new File(args[1]);File osm=pbf(new File(args[2]));
  System.setProperty("avoidMapPolling","true");
  File nodes=new File(work,"nodes"),ways=new File(work,"ways"),n55=new File(work,"n55"),w55=new File(work,"w55"),un=new File(work,"un"),seg=new File(work,"segments");
  for(File f:new File[]{nodes,ways,n55,w55,un,seg})f.mkdirs();
  File lookup=new File(profiles,"lookups.dat"),all=new File(profiles,"all.brf"),report=new File(profiles,"trekking.brf"),check=new File(profiles,"softaccess.brf");
  File border=new File(work,"border.dat"),relations=new File(work,"relations.dat"),restrictions=new File(work,"restrictions.dat"),bn=new File(work,"bordernodes.dat");
  OsmFastCutter.doCut(lookup,nodes,ways,n55,w55,border,relations,restrictions,all,report,check,osm,null);
  new PosUnifier().process(n55,un,border,bn,work.getAbsolutePath(),null);
  new WayLinker().process(un,w55,bn,restrictions,lookup,all,seg,"rd5");
  System.out.println("STRICT connected: "+route("cycleways-strict.brf",8.701,50,8.699,50,true));
  System.out.println("CONTROL connected via ordinary road: "+route("trekking.brf",8.701,50,8.707,50,true));
  System.out.println("STRICT disconnected by ordinary road: "+route("cycleways-strict.brf",8.701,50,8.707,50,false));
  System.out.println("STRICT endpoint off network: "+route("cycleways-strict.brf",8.701,50,8.700,50,false));
  System.out.println("PASS 4 full BRouter graph-routing scenarios");
 }
}
