import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.osmand.render.*;

/** Evaluates the generated rules with OsmAnd's real Java renderer. */
public class VerifyWidthModes extends VerifyStyle {
 static int checks=0;
 static void close(float actual,float expected,String label){
  checks++;
  if(Math.abs(actual-expected)>.001f)throw new AssertionError(label+": "+actual+" != "+expected);
 }
 static void equal(Object actual,Object expected,String label){
  checks++;
  if(!Objects.equals(actual,expected))throw new AssertionError(label+": "+actual+" != "+expected);
 }
 static Map<String,String> drawing(RenderingRulesStorage s,RenderingRuleSearchRequest r){
  Map<String,String> result=new TreeMap<>();
  for(String stem:new String[]{"color","strokeWidth","pathEffect","cap"})for(int layer=-2;layer<=7;layer++){
   String name=stem+(layer==0?"":layer<0?"__"+(-layer):"_"+layer);
   var p=s.PROPS.get(name);
   if(p!=null&&r.isSpecified(p))result.put(name,stem.equals("color")?color(s,r,name):
    stem.equals("strokeWidth")?Float.toString(r.getFloatPropertyValue(p)):r.getStringPropertyValue(p));
  }
  // Layer 0 is distinct from the unnumbered stroke.
  for(String name:new String[]{"color_0","strokeWidth_0","pathEffect_0","cap_0"}){
   var p=s.PROPS.get(name);
   if(p!=null&&r.isSpecified(p))result.put(name,name.startsWith("color")?color(s,r,name):
    name.startsWith("strokeWidth")?Float.toString(r.getFloatPropertyValue(p)):r.getStringPropertyValue(p));
  }
  return result;
 }
 public static void main(String[]args)throws Exception{
  dir=Path.of(args[0]);
  var rows=JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonArray();
  for(String name:new String[]{"CycloRoutePersonalBase","CycloRoutePersonalMVP"}){
   var doc=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(dir.resolve(name+".render.xml").toFile());
   var cs=doc.getElementsByTagName("renderingConstant");
   for(int i=0;i<cs.getLength();i++){var e=(org.w3c.dom.Element)cs.item(i);constants.put(e.getAttribute("name"),e.getAttribute("value"));}
  }
  var s=load("CycloRoutePersonalMVP");
  var property=s.PROPS.get("cyclorouteBikeWidth");
  equal(property.getName(),"Bicycle line width","setting title");
  equal(property.getDefaultValueDescription(),"Standard","default title");
  equal(Arrays.asList(property.getPossibleValues()),List.of("Thicker","Maximum"),"two nondefault options");
  String[][] facilities={
   {"highway","cycleway","strokeWidth_5","strokeWidth_4","#174BCC"},
   {"highway","path","strokeWidth_5","strokeWidth_4","#4A9FD9","bicycle=designated","foot=designated"},
   {"highway","footway","strokeWidth_5","strokeWidth_4","#4A9FD9","bicycle=designated"},
   {"highway","cycleway","strokeWidth_5","strokeWidth_4","#4A9FD9","foot=designated"},
   {"highway","residential","strokeWidth_5","","#7DB1D4","bicycle_road=yes"},
   {"highway","residential","strokeWidth_5","","#7DB1D4","cyclestreet=yes"},
   {"cycleway","track","strokeWidth","strokeWidth_0","#174BCC"},
   {"cycleway","lane","strokeWidth","strokeWidth_0","#6C91EF"},
   {"cycleway","shared_lane","strokeWidth","strokeWidth_0","#7DC8D4"},
   {"cycleway","share_busway","strokeWidth","strokeWidth_0","#7DC8D4"},
   {"cycleway:both","track","strokeWidth","strokeWidth_0","#174BCC"},
   {"cycleway:both","lane","strokeWidth","strokeWidth_0","#6C91EF"},
   {"cycleway:both","shared_lane","strokeWidth","strokeWidth_0","#7DC8D4"},
   {"cycleway:both","share_busway","strokeWidth","strokeWidth_0","#7DC8D4"},
   {"cycleway","opposite_track","strokeWidth","strokeWidth_0","#174BCC"},
   {"cycleway","opposite_lane","strokeWidth","strokeWidth_0","#6C91EF"}
  };
  String[] modes={"","Thicker","Maximum","Толще","Максимальная"};float[] factors={1,1.5f,2,1.5f,2};
  for(boolean night:new boolean[]{false,true})for(int z=10;z<=19;z++){
   float base=Float.NaN;
   for(var row:rows){var o=row.getAsJsonObject();if(z>=o.get("min").getAsInt()&&(!o.has("max")||z<=o.get("max").getAsInt()))base=o.get("width").getAsFloat();}
   if(Float.isNaN(base))throw new AssertionError("Missing configured zoom "+z);
   for(int m=0;m<modes.length;m++){
    bikeWidth=modes[m];float width=base*factors[m];
    for(String[] f:facilities){
     var r=query(s,f[0],f[1],z,night,Arrays.copyOfRange(f,5,f.length));
     // A bicycle street inherits its road's existing visibility threshold.
     // Width selection must not make a previously hidden road appear.
     if(f[4].equals("#7DB1D4")&&!r.isSpecified(s.PROPS.get(f[2]))){
      bikeWidth="";var hidden=drawing(s,query(s,f[0],f[1],z,night,Arrays.copyOfRange(f,5,f.length)));
      bikeWidth=modes[m];equal(drawing(s,r),hidden,"hidden bicycle street");continue;
     }
     close(r.getFloatPropertyValue(s.PROPS.get(f[2])),width,f[0]+"="+f[1]+" z"+z+" "+bikeWidth);
     equal(color(s,r,f[2].equals("strokeWidth")?"color":"color_5"),f[4],"facility colour");
     if(!f[3].isEmpty())close(r.getFloatPropertyValue(s.PROPS.get(f[3])),width+2,"casing");
    }
    for(String extra:new String[]{"bridge=yes","tunnel=yes","segregated=yes","segregated=no"}){
     var r=query(s,"highway","path",z,night,"bicycle=designated","foot=designated",extra);
     close(r.getFloatPropertyValue(s.PROPS.get("strokeWidth_5")),width,extra);
     equal(r.getStringPropertyValue(s.PROPS.get("pathEffect_5")),"","mixed path remains solid");
    }
    for(String road:new String[]{"motorway","trunk","primary","secondary","tertiary","residential","service","track","path","footway"}){
     bikeWidth="";var normal=drawing(s,query(s,"highway",road,z,night));
     bikeWidth=modes[m];equal(drawing(s,query(s,"highway",road,z,night)),normal,"ordinary road "+road);
    }
    if(z>=13)for(String road:new String[]{"residential","cycleway"}){
     bikeWidth="";var baseline=query(s,"highway",road,z,night,"bicycle=no");
     String prohibition=color(s,baseline,"color_5");float prohibitionWidth=baseline.getFloatPropertyValue(s.PROPS.get("strokeWidth_5"));
     String pattern=baseline.getStringPropertyValue(s.PROPS.get("pathEffect_5"));
     bikeWidth=modes[m];var r=query(s,"highway",road,z,night,"bicycle=no");
     equal(color(s,r,"color_5"),prohibition,"prohibition colour");
     close(r.getFloatPropertyValue(s.PROPS.get("strokeWidth_5")),prohibitionWidth,"prohibition width");
     equal(r.getStringPropertyValue(s.PROPS.get("pathEffect_5")),pattern,"prohibition ticks");
    }
    bikeWidth="";var route=new RenderingRuleSearchRequest(s);route.setBooleanFilter(s.PROPS.R_NIGHT_MODE,night);route.searchRenderingAttribute("route");
    var expected=drawing(s,route);
    route=new RenderingRuleSearchRequest(s);route.setBooleanFilter(s.PROPS.R_NIGHT_MODE,night);route.setStringFilter(property,modes[m]);route.searchRenderingAttribute("route");
    equal(drawing(s,route),expected,"navigation unaffected");
   }
  }
  // An unset/unknown preference retains the old widths rather than hiding lines.
  bikeWidth="";float defaultWidth=query(s,"highway","cycleway",17,false).getFloatPropertyValue(s.PROPS.get("strokeWidth_5"));
  bikeWidth="future-value";close(query(s,"highway","cycleway",17,false).getFloatPropertyValue(s.PROPS.get("strokeWidth_5")),defaultWidth,"unknown setting fallback");
  System.out.println("PASS "+checks+" width-mode rendering checks (all categories, zoom 10–19, day/night)");
 }
}
