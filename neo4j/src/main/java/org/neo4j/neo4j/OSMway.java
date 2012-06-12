package org.neo4j.neo4j;

public class OSMway 
{
	private String nd;
	private String tag;
	private String k;
	private String v;
	
	public String getNd() {
		return nd;
	}
	public void setNd(String nd) {
		this.nd = nd;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getK() {
		return k;
	}
	public void setK(String k) {
		this.k = k;
	}
	public String getV() {
		return v;
	}
	public void setV(String v) {
		this.v = v;
	}
	
	@Override
	public String toString() {
		return "OSMway [nd=" + nd + ", tag=" + tag + ", k="
				+ k + ", v=" + v + "]";
	}
	
}//end OSMway
