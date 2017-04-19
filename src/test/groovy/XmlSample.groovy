/**
 * Created by Administrator on 2017/4/19.
 */


def eachXmlNode(idItems,Node node,int level){
    def out=new StringBuilder()
    def nodeInfo="${node.name()}["
    out.append(nodeInfo.padLeft(nodeInfo.length()+level,"\t"))
    if(node.attributes()){
        node.attributes().each { out.append("$it.key.localPart=$it.value,") }
        //记录自定义id节点
        def findAttribute=node.attributes().find {it.key.localPart=="id"}
        !findAttribute?: idItems<<[(node.name()):findAttribute.value]
        out.deleteCharAt(out.length()-1)
        out.append("]")
    }
    println out.toString()
    if(node.children()){
        node.children().each {eachXmlNode(idItems,it,level+1)}
    }
}
//def idItems=[:]
//def root=new XmlParser().parse(new File("activity_date.xml"))
//eachXmlNode(idItems,root,0)
//println idItems

def v1="@+id/wheel_view3"
def v2="@id/wheel_view3"
def v3="@android:id/wheel_view3"
def pattern=/@[(\+id)|(id)|(android:id)]\/(.+)/
def matcher=v1=~pattern
println "分类1:${matcher[0][1]} ${matcher[0][2]} ${matcher[0][3]} ${matcher[0][4]}"

matcher=v2=~pattern
println "分类2:${matcher[0][2]} ${matcher[0][3]} ${matcher[0][4]} ${matcher[0][5]}"

matcher=v3=~pattern
println "分类3:${matcher[0][2]} ${matcher[0][3]} ${matcher[0][4]} ${matcher[0][5]}"




