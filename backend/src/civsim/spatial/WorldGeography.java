package civsim.spatial;

import java.util.List;

/** Deterministic simulation geography aligned to the Map 1 1000x1000 atlas. */
public final class WorldGeography {
    public static final double WIDTH = 1000.0;
    public static final double HEIGHT = 1000.0;
    public static final double CORE_MIN = 450.0;
    public static final double CORE_MAX = 550.0;

    public enum Terrain { OCEAN, GRASSLAND, FERTILE_GRASSLAND, FOREST, HIGHLAND, TUNDRA, WETLAND, DESERT, VOLCANIC, LAKE, RIVER }
    public record Point2D(double x, double y) {}
    private record Ellipse(double x, double y, double rx, double ry, Terrain terrain) {
        boolean contains(double px, double py) { double dx=(px-x)/rx, dy=(py-y)/ry; return dx*dx+dy*dy<=1.0; }
    }
    private record Point(double x, double y) {}

    private static final double[] LAKES = {
            230,262,20,13, 292,322,22,14, 345,212,14,10, 471,336,22,15,
            727,408,16,11, 500,598,6,5, 330,128,26,11, 650,112,22,10,
            440,70,16,7, 420,850,12,7, 575,885,9,6, 610,330,14,9
    };
    // Biome masks mirror the centers and radii in gen_map.py; the static map remains presentation only.
    private static final List<Ellipse> BIOMES = List.of(
            new Ellipse(775,815,82,112,Terrain.VOLCANIC), new Ellipse(420,865,240,95,Terrain.DESERT),
            new Ellipse(250,725,105,120,Terrain.WETLAND), new Ellipse(500,85,330,112,Terrain.TUNDRA),
            new Ellipse(500,225,255,52,Terrain.HIGHLAND), new Ellipse(750,405,100,150,Terrain.HIGHLAND),
            new Ellipse(745,615,36,105,Terrain.HIGHLAND), new Ellipse(592,432,28,28,Terrain.HIGHLAND),
            new Ellipse(413,470,22,32,Terrain.HIGHLAND), new Ellipse(275,275,135,125,Terrain.FOREST),
            new Ellipse(655,750,150,100,Terrain.FOREST), new Ellipse(410,405,38,30,Terrain.FOREST),
            new Ellipse(585,595,34,40,Terrain.FOREST), new Ellipse(380,615,32,28,Terrain.FOREST),
            new Ellipse(640,380,32,30,Terrain.FOREST), new Ellipse(565,330,30,24,Terrain.FOREST),
            new Ellipse(415,525,26,30,Terrain.FOREST), new Ellipse(700,615,28,30,Terrain.FOREST),
            new Ellipse(800,650,24,30,Terrain.FOREST), new Ellipse(360,560,70,60,Terrain.FERTILE_GRASSLAND),
            new Ellipse(800,560,46,150,Terrain.FERTILE_GRASSLAND), new Ellipse(500,470,150,185,Terrain.FERTILE_GRASSLAND)
    );
    private static final List<List<Point>> RIVERS = List.of(
            List.of(new Point(500,150),new Point(480,300),new Point(450,450),new Point(350,650),new Point(250,800)),
            List.of(new Point(780,250),new Point(730,400),new Point(650,500),new Point(600,650),new Point(700,800)),
            List.of(new Point(300,250),new Point(280,400),new Point(350,550),new Point(300,700)),
            List.of(new Point(500,600),new Point(550,700),new Point(600,800),new Point(650,900)),
            List.of(new Point(471,352),new Point(500,450),new Point(550,500),new Point(600,650))
    );
    private static final double[][] COAST_BUMPS = {{165,7,-.10},{205,5,.06},{236,8,-.09},{300,6,.05},{330,7,-.10},
            {15,6,-.10},{38,5,.07},{72,7,-.08},{110,5,.06},{136,6,-.07},{255,5,-.07},{185,4,.05},{350,4,.04},{92,4,-.06}};

    private WorldGeography() {}

    /** Maps the original [0,100] simulation plane into Map 1 without scaling distances. */
    public static Point2D legacyToMap(double x, double y) { return new Point2D(CORE_MIN + x, CORE_MAX - y); }
    public static Point2D mapToLegacy(double x, double y) { return new Point2D(x - CORE_MIN, CORE_MAX - y); }
    public static boolean inBounds(double x, double y) { return Double.isFinite(x)&&Double.isFinite(y)&&x>=0&&x<=WIDTH&&y>=0&&y<=HEIGHT; }

    public static Terrain terrainAt(double x, double y) {
        if (!inBounds(x,y)||!isLand(x,y)) return Terrain.OCEAN;
        if (isLake(x,y)) return Terrain.LAKE;
        for (Ellipse region:BIOMES) if(region.contains(x,y)) return region.terrain;
        if (nearRiver(x,y,4.0)) return Terrain.RIVER;
        return Terrain.GRASSLAND;
    }

    public static Biome biomeAt(double x,double y) {
        if(x>=CORE_MIN&&x<=CORE_MAX&&y>=CORE_MIN&&y<=CORE_MAX){Point2D legacy=mapToLegacy(x,y);return Biome.determineLegacyBiomeAt(legacy.x,legacy.y);}
        return switch(terrainAt(x,y)){
            case HIGHLAND,VOLCANIC -> Biome.HIGHLAND_MINE;
            case FOREST,WETLAND,TUNDRA -> Biome.FOREST;
            case OCEAN,LAKE -> Biome.COASTAL;
            case DESERT,FERTILE_GRASSLAND,GRASSLAND,RIVER -> Biome.RIVER_VALLEY;
        };
    }

    public static boolean isLand(double x,double y) {
        if(!inBounds(x,y)) return false;
        double dx=x-500,dy=y-500,angle=Math.atan2(dy,dx),degrees=Math.toDegrees(angle),c=Math.cos(angle),s=Math.sin(angle);
        double radius=Math.pow(Math.pow(Math.abs(c/335.0),4.2)+Math.pow(Math.abs(s/435.0),4.2),-1.0/4.2);
        return Math.hypot(dx,dy)<=radius*coastFactor(degrees,angle);
    }
    public static boolean isSettlementLocation(double x,double y) {
        return inBounds(x,y)&&isLand(x,y)&&!isLake(x,y)&&terrainAt(x,y)!=Terrain.VOLCANIC&&distanceToCoast(x,y)>=12&&!nearRiver(x,y,5);
    }
    public static boolean isAnomalyLocation(double x,double y) { return inBounds(x,y)&&isLand(x,y)&&!isLake(x,y)&&distanceToCoast(x,y)>=8; }
    public static boolean isMonsterLocation(double x,double y) { return isAnomalyLocation(x,y)&&terrainAt(x,y)!=Terrain.RIVER; }
    public static boolean isLake(double x,double y) {
        for(int i=0;i<LAKES.length;i+=4){double dx=(x-LAKES[i])/LAKES[i+2],dy=(y-LAKES[i+1])/LAKES[i+3];if(dx*dx+dy*dy<=1)return true;}return false;
    }
    public static boolean nearRiver(double x,double y,double radius) {
        for(List<Point> line:RIVERS)for(int i=1;i<line.size();i++)if(distanceToSegment(x,y,line.get(i-1),line.get(i))<=radius)return true;return false;
    }
    private static double distanceToCoast(double x,double y){double dx=x-500,dy=y-500,a=Math.atan2(dy,dx),c=Math.cos(a),s=Math.sin(a);double r=Math.pow(Math.pow(Math.abs(c/335),4.2)+Math.pow(Math.abs(s/435),4.2),-1/4.2);return r*coastFactor(Math.toDegrees(a),a)-Math.hypot(dx,dy);}
    private static double coastFactor(double degrees,double angle){double m=1+.05*Math.sin(3*angle+1.3)+.038*Math.sin(5*angle+.4)+.026*Math.sin(8*angle+2.2)+.017*Math.sin(13*angle+.9)+.010*Math.sin(21*angle+3.1)+.006*Math.sin(34*angle+.2);for(double[] b:COAST_BUMPS){double d=((degrees-b[0]+540)%360)-180;m+=b[2]*Math.exp(-Math.pow(d/b[1],2));}return m;}
    private static double distanceToSegment(double x,double y,Point a,Point b){double dx=b.x-a.x,dy=b.y-a.y,d=dx*dx+dy*dy,t=d==0?0:Math.max(0,Math.min(1,((x-a.x)*dx+(y-a.y)*dy)/d));return Math.hypot(x-a.x-t*dx,y-a.y-t*dy);}
}
