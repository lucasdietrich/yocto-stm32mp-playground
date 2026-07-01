#!/bin/sh
# description: Description comes here....

set -e

# Source function library.
. /etc/init.d/functions

PGDIR=/var/lib/postgresql
PGHBA=$PGDIR/data/pg_hba.conf
PGCONF=$PGDIR/data/postgresql.conf

PGDATA=$PGDIR/data
PGLOG=$PGDIR/pgstartup.log

STAMP_CFG=$PGDIR/.stamp_cfg
PGEXE=/etc/init.d/postgresql-server

do_init_postgresql() {
   if [ ! -f $STAMP_CFG ]; then
      if [ -d $PGDIR ]; then
         rm -rf $PGDIR
      fi

      mkdir -p $PGDIR
      chown postgres:postgres $PGDIR

      su -s /bin/sh postgres -c "/usr/bin/initdb --pgdata='$PGDATA' --auth='ident'" >> "$PGLOG" 2>&1 < /dev/null

      # # prepend to the file: allow any local (socket) connections
      # sed -i 's/^local[[:space:]]\+all[[:space:]]\+all[[:space:]]\+peer/local all all trust/' $PGHBA

      # # remove default entries
      # sed -i '/^host[[:space:]]\+all[[:space:]]\+all[[:space:]]\+127\.0\.0\.1\/32[[:space:]]\+ident/d' $PGHBA
      # sed -i '/^host[[:space:]]\+all[[:space:]]\+all[[:space:]]\+::1\/128[[:space:]]\+ident/d' $PGHBA

      # # in development image, listen on all interfaces and trust all connections (local and remote)
      # if [ -f "/etc/.ha-debug-image" ]; then
      #    echo "listen_addresses = '*'" >> $PGCONF

      #    # allow any external (IP) connection
      #    echo "host all all 0.0.0.0/0 trust" >> $PGHBA
      #    echo "host all all ::0/0 trust" >> $PGHBA
      # else
      #    echo "listen_addresses = 'localhost'" >> $PGCONF

      #    echo "host all all samenet trust" >> $PGHBA
      #    echo "host all all samehost trust" >> $PGHBA
      # fi

      # mark init script as executed
      touch $STAMP_CFG
   fi
}

start() {
   do_init_postgresql
   $PGEXE start
}

reset() {
   $PGEXE stop
   rm $STAMP_CFG
   start
   $PGEXE start
}

case "$1" in 
    start)
       start
       ;;
    reset)
       stop
       start
       ;;
    *)
       echo "Usage: $0 {start|reset}"
esac

exit 0 