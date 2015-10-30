# -*- coding: utf-8 -*-
"""
    domains.rom
    ~~~~~~~~~~~~~~~~~~~~~~~~~

    The Kurento Remote Object Model (ROM) domain.

    Mostly cloned from the standard sphinx js role.
    We added .. event::, :evnt: .. enum::, :enum:,
    .. record::, and :rcrd:

    :copyright: Copyright 2014 by the Kurento team.
    :license: LGPL 2.1, see LICENSE for details.
"""

from sphinx import addnodes
from sphinx.domains import Domain, ObjType
from sphinx.locale import l_, _
from sphinx.directives import ObjectDescription
from sphinx.roles import XRefRole
from sphinx.domains.python import _pseudo_parse_arglist
from sphinx.util.nodes import make_refnode
from sphinx.util.docfields import Field, GroupedField, TypedField


class RemoteObject(ObjectDescription):
    """
    Description of a Kurento ROM object.
    """
    #: If set to ``True`` this object is callable and a `desc_parameterlist` is
    #: added
    has_arguments = False

    #: what is displayed right before the documentation entry
    display_prefix = None

    def handle_signature(self, sig, signode):
        sig = sig.strip()
        if '(' in sig and sig[-1:] == ')':
            prefix, arglist = sig.split('(', 1)
            prefix = prefix.strip()
            arglist = arglist[:-1].strip()
        else:
            prefix = sig
            arglist = None
        if '.' in prefix:
            nameprefix, name = prefix.rsplit('.', 1)
        else:
            nameprefix = None
            name = prefix

        objectname = self.env.temp_data.get('rom:object')
        if nameprefix:
            if objectname:
                # someone documenting the method of an attribute of the current
                # object? shouldn't happen but who knows...
                nameprefix = objectname + '.' + nameprefix
            fullname = nameprefix + '.' + name
        elif objectname:
            fullname = objectname + '.' + name
        else:
            # just a function or constructor
            objectname = ''
            fullname = name

        signode['object'] = objectname
        signode['fullname'] = fullname

        if self.display_prefix:
            signode += addnodes.desc_annotation(self.display_prefix,
                                                self.display_prefix)
        if nameprefix:
            signode += addnodes.desc_addname(nameprefix + '.', nameprefix + '.')
        signode += addnodes.desc_name(name, name)
        if self.has_arguments:
            if not arglist:
                signode += addnodes.desc_parameterlist()
            else:
                _pseudo_parse_arglist(signode, arglist)
        return fullname, nameprefix

    def add_target_and_index(self, name_obj, sig, signode):
        objectname = self.options.get(
            'object', self.env.temp_data.get('rom:object'))
        fullname = name_obj[0]
        if fullname not in self.state.document.ids:
            signode['names'].append(fullname)
            signode['ids'].append(fullname.replace('$', '_S_'))
            signode['first'] = not self.names
            self.state.document.note_explicit_target(signode)
            objects = self.env.domaindata['rom']['objects']
            if fullname in objects:
                self.state_machine.reporter.warning(
                    'duplicate object description of %s, ' % fullname +
                    'other instance in ' +
                    self.env.doc2path(objects[fullname][0]),
                    line=self.lineno)
            objects[fullname] = self.env.docname, self.objtype

        indextext = self.get_index_text(objectname, name_obj)
        if indextext:
            self.indexnode['entries'].append(('single', indextext,
                                              fullname.replace('$', '_S_'),
                                              ''))

    def get_index_text(self, objectname, name_obj):
        name, obj = name_obj
        if self.objtype == 'method':
            if not obj:
                return _('%s() (built-in method)') % name
            return _('%s() (ROM %s method)') % (name, obj)
        elif self.objtype == 'constructor':
            if not obj:
                return _('%s() (built-in constructor)') % name
            return _('%s() (ROM %s constructor)') % (name, obj)
        elif self.objtype == 'class':
            return _('%s (ROM class)') % name
        elif self.objtype == 'data':
            return _('%s (global variable or constant)') % name
        elif self.objtype == 'attribute':
            return _('%s (%s attribute)') % (name, obj)
        elif self.objtype == 'event':
            return _('%s (ROM event)') % name
        return ''


class RemoteCallable(RemoteObject):
    """Description of a Kurento ROM function, method or constructor."""
    has_arguments = True

    doc_field_types = [
        TypedField('arguments', label=l_('Arguments'),
                   names=('argument', 'arg', 'parameter', 'param'),
                   typerolename='func', typenames=('paramtype', 'type')),
        GroupedField('errors', label=l_('Throws'), rolename='err',
                     names=('throws', ),
                     can_collapse=True),
        GroupedField('events', label=l_('Events'), rolename='evt',
                     names=('events', ),
                     can_collapse=True),
        Field('returnvalue', label=l_('Returns'), has_arg=False,
              names=('returns', 'return')),
        Field('returntype', label=l_('Return type'), has_arg=False,
              names=('rtype',)),
        Field('isabstract', label=l_('Abstract'), has_arg=False,
              names=('abstract',)),
    ]


class RemoteObjectConstructor(RemoteCallable):
    """Like a callable but with a different prefix."""
    display_prefix = ''


class ROMXRefRole(XRefRole):
    def process_link(self, env, refnode, has_explicit_title, title, target):
        # basically what sphinx.domains.python.PyXRefRole does
        refnode['rom:object'] = env.temp_data.get('rom:object')
        if not has_explicit_title:
            title = title.lstrip('.')
            target = target.lstrip('~')
            if title[0:1] == '~':
                title = title[1:]
                dot = title.rfind('.')
                if dot != -1:
                    title = title[dot+1:]
        if target[0:1] == '.':
            target = target[1:]
            refnode['refspecific'] = True
        return title, target


class ROMDomain(Domain):
    """Kurento Remote Object language domain."""
    name = 'rom'
    label = 'Kurento ROM'
    # if you add a new object type make sure to edit RemoteObject.get_index_text
    object_types = {
        'method':      ObjType(l_('method'),      'meth'),
        'class':       ObjType(l_('class'),       'cls'),
        'record':      ObjType(l_('record'),      'rec'),
        'enum':        ObjType(l_('enum'),        'enum'),
        'constructor': ObjType(l_('constructor'), 'ctrc'),
        'data':        ObjType(l_('data'),        'data'),
        'attribute':   ObjType(l_('attribute'),   'attr'),
        'event':       ObjType(l_('event'),       'evnt'),
    }
    directives = {
        'method':      RemoteCallable,
        'class':       RemoteObject,
        'record':      RemoteObject,
        'enum':        RemoteObject,
        'constructor': RemoteObjectConstructor,
        'data':        RemoteObject,
        'attribute':   RemoteObject,
        'event':       RemoteCallable,
    }
    roles = {
        'meth':  ROMXRefRole(fix_parens=True),
        'cls':   ROMXRefRole(fix_parens=True),
        'rec':   ROMXRefRole(fix_parens=True),
        'enum':  ROMXRefRole(fix_parens=True),
        'ctrc':  ROMXRefRole(fix_parens=True),
        'data':  ROMXRefRole(),
        'attr':  ROMXRefRole(),
        'evnt':  ROMXRefRole(fix_parens=True),
    }
    initial_data = {
        'objects': {}, # fullname -> docname, objtype
    }

    def clear_doc(self, docname):
        for fullname, (fn, _) in self.data['objects'].items():
            if fn == docname:
                del self.data['objects'][fullname]

    def find_obj(self, env, obj, name, typ, searchorder=0):
        if name[-2:] == '()':
            name = name[:-2]
        objects = self.data['objects']
        newname = None
        if searchorder == 1:
            if obj and obj + '.' + name in objects:
                newname = obj + '.' + name
            else:
                newname = name
        else:
            if name in objects:
                newname = name
            elif obj and obj + '.' + name in objects:
                newname = obj + '.' + name
        return newname, objects.get(newname)

    def resolve_xref(self, env, fromdocname, builder, typ, target, node,
                     contnode):
        objectname = node.get('rom:object')
        searchorder = node.hasattr('refspecific') and 1 or 0
        name, obj = self.find_obj(env, objectname, target, typ, searchorder)
        if not obj:
            return None
        return make_refnode(builder, fromdocname, obj[0],
                            name.replace('$', '_S_'), contnode, name)

    def get_objects(self):
        for refname, (docname, type) in self.data['objects'].iteritems():
            yield refname, refname, type, docname, \
                  refname.replace('$', '_S_'), 1
