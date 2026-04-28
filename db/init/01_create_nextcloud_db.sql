DO
$$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'nextcloud') THEN
    CREATE DATABASE nextcloud OWNER app;
  END IF;
END
$$;

