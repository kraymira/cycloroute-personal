import java.nio.file.*;
import net.osmand.render.*;
public class VerifyMvpStyle extends VerifyStyle {
 public static void main(String[]args)throws Exception{
  dir=Path.of(args[0]);
  for(String n:new String[]{"CycloRoutePersonalBase","CycloRoutePersonalMVP"}){
   var doc=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(dir.resolve(n+".render.xml").toFile());
   var cs=doc.getElementsByTagName("renderingConstant");
   for(int i=0;i<cs.getLength();i++){var e=(org.w3c.dom.Element)cs.item(i);constants.put(e.getAttribute("name"),e.getAttribute("value"));}
  }
  var s=load("CycloRoutePersonalMVP");int count=0;
  for(boolean night:new boolean[]{false,true}){
   var r=new RenderingRuleSearchRequest(s);r.setBooleanFilter(s.PROPS.R_NIGHT_MODE,night);
   if(!r.searchRenderingAttribute("route"))throw new AssertionError("Missing route");
   expect(r.getStringPropertyValue(s.PROPS.get("pathEffect")),"5_7","route gaps");
   expect(color(s,r,"color"),"#C03EAD","route colour");
   if(Math.abs(r.getFloatPropertyValue(s.PROPS.get("strokeWidth"))-.7f)>.001f)throw new AssertionError("Route too wide");count+=3;
   expect(color(s,query(s,"highway","cycleway",17,night),"color_5"),"#174BCC","cycleway");count++;
   expect(color(s,query(s,"cycleway","lane",17,night),"color"),"#6C91EF","lane");count++;
  }
  System.out.println("PASS "+count+" MVP route-style checks using OsmAnd Java renderer (no Android visual claim)");
 }
}
