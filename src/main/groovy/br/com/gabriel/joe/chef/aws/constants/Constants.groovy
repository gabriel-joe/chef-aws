package br.com.gabriel.joe.chef.aws.constants

class Constants {

	static final String ACTIONS_DATABASE_INSTALL = "install.database.atualizada"
	static final String ACTIONS_DATABASE_DOWNLOAD = "download.script.com.sucesso"
	static final String[] ACTIONS_DATABASE = [ACTIONS_DATABASE_DOWNLOAD, ACTIONS_DATABASE_INSTALL]
	
	
	static final String COMMAND_GRANT_SYNONYMS = """
	DECLARE
	  --##############################################################################
	  --##############################################################################
	  -- cursor cGrants
	  --##############################################################################
	  --##############################################################################
	  CURSOR cGrants
	  IS
	    SELECT comando
	    FROM
	      (
	      --##############################################################################
	      -- grants para tabelas (REVISADO)
	      --##############################################################################
	      SELECT 'GRANT SELECT,UPDATE,DELETE,INSERT ON '
	        ||d.owner
	        ||'.'
	        ||d.object_name
	        ||' TO '
	        ||(
	        CASE
	          WHEN d.owner = 'DBAMV'
	          THEN 'DBAPS,DBASGU,MVINTEGRA,MV2000'
	          WHEN d.owner = 'DBAPS'
	          THEN 'DBAMV,DBASGU,MVINTEGRA,MV2000'
	          WHEN d.owner = 'DBASGU'
	          THEN 'DBAMV,DBAPS,MVINTEGRA,MV2000'
	          WHEN d.owner = 'MVINTEGRA'
	          THEN 'DBAMV,DBAPS,DBASGU,MV2000'
	        END ) COMANDO
	      FROM DBA_OBJECTS d
	      WHERE D.OWNER     IN( 'DBAMV', 'DBASGU', 'DBAPS', 'MVINTEGRA' )
	      AND d.object_type IN( 'VIEW' )
	      AND d.object_name NOT LIKE '%BIN\$%'
	      AND ((SELECT COUNT(*)
	        FROM DBA_TAB_PRIVS TP
	        WHERE TP.TABLE_NAME = D.OBJECT_NAME
	        AND TP.OWNER        = D.OWNER
	        AND tp.grantee     IN( 'DBAMV', 'DBASGU', 'DBAPS', 'MVINTEGRA' )
	        AND TP.PRIVILEGE   <> 'EXECUTE' ) < 12)
	      UNION ALL
	      --##############################################################################
	      -- grants para views (REVISADO)
	      --##############################################################################
	      SELECT 'GRANT SELECT,UPDATE,DELETE,INSERT ON '
	        ||d.owner
	        ||'.'
	        ||d.object_name
	        ||' TO '
	        ||(
	        CASE
	          WHEN d.owner = 'DBAMV'
	          THEN 'DBAPS,DBASGU,MVINTEGRA,MV2000'
	          WHEN d.owner = 'DBAPS'
	          THEN 'DBAMV,DBASGU,MVINTEGRA,MV2000'
	          WHEN d.owner = 'DBASGU'
	          THEN 'DBAMV,DBAPS,MVINTEGRA,MV2000'
	          WHEN d.owner = 'MVINTEGRA'
	          THEN 'DBAMV,DBAPS,DBASGU,MV2000'
	        END ) COMANDO
	      FROM DBA_OBJECTS d
	      WHERE D.OWNER     IN( 'DBAMV', 'DBASGU', 'DBAPS', 'MVINTEGRA' )
	      AND d.object_type IN( 'VIEW' )
	      AND d.object_name NOT LIKE '%BIN\$%'
	      AND ((SELECT COUNT(*)
	        FROM DBA_TAB_PRIVS TP
	        WHERE TP.TABLE_NAME = D.OBJECT_NAME
	        AND TP.OWNER        = D.OWNER
	        AND tp.grantee     IN( 'DBAMV', 'DBASGU', 'DBAPS', 'MVINTEGRA' )
	        AND TP.PRIVILEGE   <> 'EXECUTE' ) < 12)
	      UNION ALL
	      --##############################################################################
	      -- GRANTS PARA SEQUENCES (REVISADO)
	      --##############################################################################
	      SELECT 'GRANT SELECT ON '
	        ||o.owner
	        ||'.'
	        ||o.object_name
	        ||' TO '
	        ||(
	        CASE
	          WHEN o.owner = 'DBAMV'
	          THEN 'DBAPS,DBASGU,MVINTEGRA,MV2000'
	          WHEN o.owner = 'DBAPS'
	          THEN 'DBAMV,DBASGU,MVINTEGRA,MV2000'
	          WHEN o.owner = 'DBASGU'
	          THEN 'DBAMV,DBAPS,MVINTEGRA,MV2000'
	          WHEN o.owner = 'MVINTEGRA'
	          THEN 'DBAMV,DBAPS,DBASGU,MV2000'
	        END ) COMANDO
	      FROM dba_objects o
	      WHERE o.object_type = 'SEQUENCE'
	      AND o.owner        IN( 'DBAMV', 'DBAPS', 'DBASGU', 'MVINTEGRA' )
	      AND o.object_name NOT LIKE '%BIN\$%'
	      AND ( (SELECT COUNT(*)
	        FROM dba_tab_privs tp
	        WHERE tp.table_name = o.object_name
	        AND tp.privilege    = 'SELECT'
	        AND TP.OWNER        = O.OWNER
	        AND TP.GRANTEE     IN( 'DBAMV', 'DBASGU', 'DBAPS', 'MVINTEGRA', 'MV2000' ) ) < 4)
	      UNION ALL
	      --##############################################################################
	      -- GRANT EXECUTE EM FUNCTIONS, PROCEDURES, PACKAGES (REVISADO)
	      --##############################################################################
	      SELECT 'GRANT EXECUTE ON '
	        ||o.owner
	        ||'.'
	        ||o.object_name
	        ||' TO '
	        ||(
	        CASE
	          WHEN o.owner = 'DBAMV'
	          THEN 'DBAPS,DBASGU,MVINTEGRA,MV2000'
	          WHEN o.owner = 'DBAPS'
	          THEN 'DBAMV,DBASGU,MVINTEGRA,MV2000'
	          WHEN o.owner = 'DBASGU'
	          THEN 'DBAMV,DBAPS,MVINTEGRA,MV2000'
	          WHEN o.owner = 'MVINTEGRA'
	          THEN 'DBAMV,DBAPS,DBASGU,MV2000'
	        END ) COMANDO
	      FROM DBA_OBJECTS O
	      WHERE o.object_type IN( 'FUNCTION', 'PROCEDURE', 'PACKAGE' )
	      AND o.owner         IN( 'DBAMV', 'DBAPS', 'DBASGU', 'MVINTEGRA' )
	      AND o.object_name NOT LIKE '%BIN\$%'
	      AND ( (SELECT COUNT(*)
	        FROM dba_tab_privs tp
	        WHERE tp.table_name = o.object_name
	        AND tp.privilege    = 'EXECUTE'
	        AND TP.OWNER        = O.OWNER
	        AND TP.GRANTEE     IN( 'DBAMV', 'DBASGU', 'DBAPS', 'MVINTEGRA', 'MV2000' ) ) < 4)
	      );
	    --##############################################################################
	    --##############################################################################
	    -- cursor cPubSyn
	    --##############################################################################
	    --##############################################################################
	    CURSOR cPubSyn
	    IS
	      SELECT comando
	      FROM
	        (
	        --##############################################################################
	        -- CRIA sinonimos publicos para tabelas e views (REVISADO)
	        --##############################################################################
	        SELECT 'CREATE PUBLIC SYNONYM '
	          ||o.object_name
	          ||' FOR '
	          || o.owner
	          || '.'
	          ||o.object_name comando
	        FROM dba_objects o
	        WHERE O.OBJECT_TYPE IN( 'TABLE', 'VIEW' )
	        AND o.owner         IN( 'DBAMV' ) --, 'DBASGU', 'DBAPS', 'MVINTEGRA' )
	        AND O.OBJECT_NAME NOT LIKE '%BIN\$%'
	        AND o.object_name       <> 'PLAN_TABLE'
	        AND (O.OBJECT_NAME) NOT IN
	          (SELECT S.TABLE_NAME
	          FROM dba_synonyms s
	          WHERE s.owner     = 'PUBLIC'
	
	          AND S.TABLE_OWNER = 'DBAMV'
	          )
	        );
	      --##############################################################################
	      --##############################################################################
	      -- cursor cPubSynObj
	
	      --##############################################################################
	      --##############################################################################
	      CURSOR cPubSynObj
	      IS
	        --##############################################################################
	        -- CRIA sinonimos publicos para functions, procedures, packages, sequences (nao alterado)
	        --##############################################################################
	        SELECT 'CREATE PUBLIC SYNONYM '
	          || o.object_name
	          ||' FOR '
	          || o.owner
	          ||'.'
	          ||o.object_name comando
	        FROM DBA_OBJECTS O
	        WHERE O.OBJECT_TYPE IN( 'FUNCTION', 'PROCEDURE', 'PACKAGE', 'SEQUENCE' )
	        AND o.owner         IN( 'DBAMV', 'DBASGU', 'DBAPS', 'MVINTEGRA' )
	        AND O.OBJECT_NAME NOT LIKE '%BIN\$%'
	        AND NOT EXISTS
	          (SELECT 1
	          FROM dba_synonyms s
	          WHERE s.owner     = 'PUBLIC'
	          AND s.table_name  = o.object_name
	          AND S.TABLE_OWNER = O.OWNER
	          );
	      --##############################################################################
	      --##############################################################################
	    BEGIN
	      --##############################################################################
	      -- executa os comandos gerados no cursor de grants
	      --##############################################################################
	      FOR vcGrants IN cGrants
	      LOOP
	        BEGIN
	          EXECUTE IMMEDIATE( vcGrants.comando );
	        EXCEPTION
	        WHEN OTHERS THEN
	          Dbms_Output.put_line( vcGrants.comando );
	        END;
	      END LOOP;
	      --##############################################################################
	      -- executa os comandos gerados no cursor de sinonimos publicos para tabelas
	      --##############################################################################
	      FOR vcPubSyn IN cPubSyn
	      LOOP
	        BEGIN
	          EXECUTE IMMEDIATE( vcPubSyn.comando );
	        EXCEPTION
	        WHEN OTHERS THEN
	          Dbms_Output.put_line( vcPubSyn.comando );
	        END;
	      END LOOP;
	      --##############################################################################
	      -- executa os comandos gerados no cursor de sinonimos publicos de outros objetos
	      --##############################################################################
	      FOR vcPubSynObj IN cPubSynObj
	      LOOP
	        BEGIN
	          EXECUTE IMMEDIATE( vcPubSynObj.comando );
	        EXCEPTION
	        WHEN OTHERS THEN
	          Dbms_Output.put_line( vcPubSynObj.comando );
	        END;
	      END LOOP;
	    END;
	"""
	
	
	static final String QUERY_INDEX_FKS = """
				SELECT 'create index '
	  || owner
	  || '.'
	  || REPLACE(constraint_name , '_FK' , '_IX')
	  || CHR(10)
	  || ' ON '
	  || owner 
	  || '.' 
      || TABLE_NAME
	  || '('
	  || COLUNAS
	  || ') TABLESPACE MV2000_I COMPUTE STATISTICS' FKs
	FROM
	  (SELECT R.* ,
	    DC.CONSTRAINT_NAME
	  FROM
	    (SELECT *
	    FROM
	      (SELECT OWNER,
	        table_name,
	        f_lista_colunas_cons( owner, constraint_name , constraint_type) colunas
	      FROM dba_constraints
	      WHERE owner        IN ('DBAMV','DBASGU')
	      AND constraint_type = 'R'
	      ) cons
	    MINUS
	    SELECT *
	    FROM
	      (SELECT OWNER,
	        table_name,
	        f_lista_colunas_index(owner , index_name) colunas
	      FROM dba_indexes
	      WHERE owner IN ('DBAMV','DBASGU')
	      ) inde
	    ) R ,
	    DBA_CONSTRAINTS DC
	  WHERE R.TABLE_NAME     = DC.TABLE_NAME
	  AND R.COLUNAS          = f_lista_colunas_cons( DC.owner, DC.constraint_name , DC.constraint_type)
	  AND dc.constraint_type = 'R'
	  AND DC.owner          IN ('DBAMV','DBASGU')
	  )

		"""
	
		
		
	static final String ENABLE_AUDIT_TRIGGER = " ALTER TRIGGER SYSTEM.TRG_AUSSPIONIEREN ENABLE "
	
	
	static final String SELECT_AUDIT_TABLE = """SELECT * FROM (
														SELECT * FROM SYSTEM.ausspionieren
														ORDER BY timestamp desc )
													WHERE ROWNUM <= 200"""
	
													
	static final String SELECT_EXISTS_DBAMV_GCM_VERSAO = """ SELECT COUNT(*) qtd FROM ALL_OBJECTS WHERE OBJECT_NAME = 'GCM_VERSAO' AND OWNER = 'DBAMV' """
	
	
	static final String INSERT_DBAMV_GCM_VERSAO = """ INSERT INTO DBAMV.GCM_VERSAO (CD_PRODUTO, DS_VERSAO, DH_ATUALIZACAO) VALUES (?,?,SYSDATE)"""
	
	static final String DELETE_DBAMV_GCM_VERSAO = """ DELETE FROM DBAMV.GCM_VERSAO WHERE CD_PRODUTO = ? """
	
		
}
