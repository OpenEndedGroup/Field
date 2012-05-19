
def markdownForJavaClass(ed, jc):
    try:
        jc = ed.resolveJavaClass(jc, jc.getName())
        if (jc==None): return None

    

        mm = jc.getMethods()
        md = "# "+jc.getName()+"\n"
    
        if (jc.getComment()):
            md += jc.getComment()+"\n"
        q = 0
        for m in mm:
            q += 1
            if (m.getComment()):
                md += """
#### <a href="#%i">%s ( %s )</a>
[fold]

%s

[/fold]
""" % (q, m.getName(), ", ".join([n.getName() for n in m.getParameters()]), m.getComment())
        return md
    except:
        return None

