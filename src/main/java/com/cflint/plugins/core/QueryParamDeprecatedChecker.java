package com.cflint.plugins.core;

import com.cflint.BugList;
import com.cflint.CF;
import com.cflint.plugins.CFLintScannerAdapter;
import com.cflint.plugins.Context;
import net.htmlparser.jericho.Element;

import java.util.Arrays;
import java.util.List;

public class QueryParamDeprecatedChecker extends CFLintScannerAdapter {
    protected CFScopes scopes = new CFScopes();
    private List<String> query_params = Arrays.asList("cf_sql_array","cf_sql_bigint","cf_sql_binary","cf_sql_bit","cf_sql_boolean","cf_sql_blob","cf_sql_char","cf_sql_clob","cf_sql_datalink","cf_sql_date","cf_sql_distinct","cf_sql_numeric","cf_sql_decimal","cf_sql_double","cf_sql_real","cf_sql_float","cf_sql_tinyint","cf_sql_smallint","cf_sql_struct","cf_sql_integer","cf_sql_varchar","cf_sql_nvarchar","cf_sql_varchar2","cf_sql_longvarbinary","cf_sql_varbinary","cf_sql_longvarchar","cf_sql_time","cf_sql_timestamp","cf_sql_ref","cf_sql_refcursor","cf_sql_other","cf_sql_null","cf_sql_money");
    @Override
    public void element(final Element element, final Context context, final BugList bugs) {
        final String tagName = element.getName();
        if (tagName.equals(CF.CFQUERYPARAM)) {
            String cfsqltype = element.getAttributeValue("cfsqltype");
            if(cfsqltype!=null && !query_params.contains(cfsqltype.toLowerCase().trim()) && !cfsqltype.startsWith("#")){
                context.addMessage("NOT_VALID_CFQUERYPARAM", cfsqltype);
            }
        }
    }

}
