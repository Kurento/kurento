#!/usr/bin/python
import os
import sys
import subprocess
import getopt
import datetime

##### FUNCTIONS #####
def usage (message):
    print ""
    print message
    print "\n"
    print "\tbuild-openstack-image.py"
    print "\n"

##### MAIN #####
# Verify packer installation
try:
    subprocess.check_call("which packer > /dev/null", shell=True)
except:
    print "======================="
    print "Packer not found or not installed. Follow instructions at:"
    print "\thttps://www.packer.io/intro/getting-started/setup.html"
    print "======================="
    sys.exit()

# Set global variables
kurento_tools_home = os.path.dirname(__file__) + os.sep + ".."
openstack_username = None
openstack_password = None
openstack_tenant = None
openstack_url=None
source_image = None
image_name = None
image_username = None
image_flavor = None
floating_ip_pool = None
packer_template= None
chef_cookbooks = None

# Get command line arguments
try:
    opts, args = getopt.getopt(sys.argv[1:],"d",
    [ \
        "openstack-username=", \
        "openstack-password=", \
        "openstack-tenant=", \
        "openstack-url=", \
        "source-image=", \
        "image-name=", \
        "image-username=", \
        "image-flavor=", \
        "floating-ip-pool=", \
        "packer-template=", \
        "chef-cookbooks="
    ])

    for opt, arg in opts:
        if opt == "-d":
            os.environ["PACKER_LOG"]="1"
        elif opt == "--openstack-username":
            openstack_username=arg
        elif opt == "--openstack-password":
            openstack_password=arg
        elif opt == "--openstack-tenant":
            openstack_tenant=arg
        elif opt == "--openstack-url":
            openstack_url=arg
        elif opt == "--source-image":
            source_image=arg
        elif opt == "--image-name":
            image_name=arg
        elif opt == "--image-username":
            image_username=arg
        elif opt == "--image-flavor":
            image_flavor=arg
        elif opt == "--floating-ip-pool":
            floating_ip_pool=arg
        elif opt == "--packer-template":
            packer_template=arg
        elif opt == "--chef-cookbooks":
            chef_cookbooks = arg

except getopt.GetoptError as e:
    usage(e)
    sys.exit(2)

# Verify mandatory command line parameters
if openstack_username is None:
    usage("Mandatory parameter: --openstack-username")
    sys.exit(1)
if openstack_password is None:
    usage("Mandatory parameter: --openstack-password")
    sys.exit(1)
if openstack_url is None:
    usage("Mandatory paramter: --openstack-url")
    sys.exit(1)
if source_image is None:
    usage("Mandatory parameter: --source-image")
    sys.exit(1)
if floating_ip_pool is None:
    usage("Mandatory parameter: --floating-ip-pool")
    sys.exit(1)
if packer_template is None:
    usage("Mandatory parameter: --packer-template")
    sys.exit(1)
if not os.path.isfile(packer_template):
    print "Packer template: File not found"
    sys.exit(1)
if chef_cookbooks is None:
    usage("Mandatory parameter: --chef-cookbooks")
    sys.exit(1)

# Set default values
if image_name is None:
    image_name="kurento-container-" + datetime.datetime.now().strftime("%Y%m%d-%H%M%S")

# Set default variables required by openstack packer builders
os.environ["OS_AUTH_URL"] = openstack_url
os.environ["OS_USERNAME"] = openstack_username
os.environ["OS_PASSWORD"] = openstack_password
if not openstack_tenant is None:
    os.environ["OS_TENANT_NAME"] = openstack_tenant

# Create packer command
build_cmd = "packer build " \
    "-var 'chef_cookbooks=" + chef_cookbooks +"' " \
    "-var 'openstack_username=" + openstack_username + "' " \
    "-var 'openstack_password=" + openstack_password + "' " \
    "-var 'source_image=" + source_image + "' " \
    "-var 'image_name=" + image_name + "' " \
    "-var 'floating_ip_pool=" + floating_ip_pool + "' "
if not openstack_tenant is None:
    build_cmd += "-var 'opensack_tenant=" + openstack_tenant + "' "
if not image_username is None:
    build_cmd += "-var 'image_username=" + image_username + "' "
if not image_flavor is None:
    build_cmd += "-var 'image_flavor=" + image_flavor + "' "
build_cmd += packer_template

print "Execute packer command: " + build_cmd
proc = subprocess.Popen( build_cmd, stdout=subprocess.PIPE, shell=True)
(out, err) = proc.communicate()
print "STDOUT:",out
