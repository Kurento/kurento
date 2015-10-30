import rom

from docutils import nodes, utils
from functools import wraps
from sphinx.util.nodes import split_explicit_title
from urllib import quote
from xml.sax import saxutils


"""
wikipedia taken from BSD Licensed

https://github.com/MiCHiLU/sphinxcontrib-externallinks/

"""
def html_escape(value):
    return saxutils.escape(value, {"\'":"&apos;", "\"":"&quot;"})

def gen_role(func, use_explicit=False):

    @wraps(func)
    def role(typ, rawtext, text, lineno, inliner, options={}, content=[]):
        has_explicit, title, other = split_explicit_title(utils.unescape(text))
        args = [other]
        if use_explicit:
            args.append(has_explicit)
        result = func(*args)
        if isinstance(result, (list, tuple)):
            url, title = result
        else:
            url = result
        node = nodes.raw("", u"<a href='{url}'>{title}</a>".format(title=title, url=html_escape(url)), format="html")
        return [node], []

    return role
def wikipedia(word, has_title=False):
    try:
        l, q = word.split(",", 1)
    except:
        l, q = 'en', word
    url = u"http://{lang}.wikipedia.org/wiki/{query}".format(lang=l, query=quote(q))
    if has_title:
        return url
    else:
        return url, q

def setup(app):
    app.add_domain(rom.ROMDomain)
    app.add_role("wikipedia", gen_role(wikipedia, True))

