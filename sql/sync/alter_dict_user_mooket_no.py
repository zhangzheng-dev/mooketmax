import psycopg2
import sys

try:
    conn = psycopg2.connect(
        host='43.139.56.124',
        port=30032,
        dbname='mooket_db',
        user='mooketmax_dba',
        password='MooketMax@2024!'
    )
    cur = conn.cursor()
    cur.execute("ALTER TABLE dict_user ADD COLUMN IF NOT EXISTS mooket_no VARCHAR(100);")
    conn.commit()
    cur.close()
    conn.close()
    print('SUCCESS: mooket_no column added')
except Exception as e:
    print(f'ERROR: {e}')
    sys.exit(1)
