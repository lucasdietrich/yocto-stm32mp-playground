# rust: sqlx-sqlite crate depends on "unlock_notify" feature of crate 
# libsqlite3-sys which uses 'sqlite3_unlock_notify()' symbol.
CFLAGS += " -DSQLITE_ENABLE_UNLOCK_NOTIFY=1"

# rust: in some cases sqlite3-sys is also built for the host
BUILD_CFLAGS += " -DSQLITE_ENABLE_UNLOCK_NOTIFY=1"