import React from "react";
import { Box, Flex } from "grid-styled";
import styled from "styled-components";
import { jt } from "ttag";

import * as Urls from "metabase/lib/urls";
import { color, lighten } from "metabase/lib/colors";

import Icon from "metabase/components/Icon";
import Link from "metabase/components/Link";
import Text from "metabase/components/type/Text";

import Database from "metabase/entities/databases";
import Table from "metabase/entities/tables";

function colorForType(props) {
  if (props.item.collection_position) {
    return color("warning");
  }
  switch (props.type) {
    case "collection":
      return lighten("brand", 0.35);
    default:
      return color("brand");
  }
}

const IconWrapper = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: ${colorForType};
  margin-right: 10px;
  flex-shrink: 0;
`;

const ResultLink = styled(Link)`
  display: block;
  background-color: transparent;
  min-height: ${props => (props.compact ? "36px" : "54px")};
  padding-top: 8px;
  padding-bottom: 8px;
  padding-left: 14px;
  padding-right: ${props => (props.compact ? "20px" : "32px")};

  &:hover {
    background-color: ${lighten("brand", 0.63)};

    h3 {
      color: ${color("brand")};
    }
  }

  ${Link} {
    text-underline-position: under;
    text-decoration: underline ${color("text-light")};
    text-decoration-style: dashed;
    &:hover {
      color: ${color("brand")};
      text-decoration-color: ${color("brand")};
    }
  }

  ${Text} {
    margin-top: 0;
    margin-bottom: 0;
    font-size: 13px;
    line-height: 19px;
  }

  h3 {
    font-size: ${props => (props.compact ? "14px" : "16px")};
    line-height: 1.2em;
    word-wrap: break-word;
    margin-bottom: 0;
  }

  .Icon-info {
    color: ${color("text-light")};
  }
`;

function ItemIcon({ item, type }) {
  return (
    <IconWrapper item={item} type={type}>
      {type === "table" ? (
        <Icon name="database" />
      ) : (
        <Icon name={item.getIcon()} size={20} />
      )}
    </IconWrapper>
  );
}

export default function SearchResult(props) {
  const { result } = props;
  switch (result.model) {
    case "card":
      return <QuestionResult question={result} options={props} />;
    case "collection":
      return <CollectionResult collection={result} options={props} />;
    case "dashboard":
      return <DashboardResult dashboard={result} options={props} />;
    case "table":
      return <TableResult table={result} options={props} />;
    case "segment":
      return <SegmentResult segment={result} options={props} />;
    case "metric":
      return <MetricResult metric={result} options={props} />;
    default:
      return <DefaultResult result={result} options={props} />;
  }
}

function TableResult({ table, options }) {
  return (
    <ResultLink to={table.getUrl()} compact={options.compact}>
      <Flex align="start">
        <ItemIcon item={table} type="table" />
        <Box>
          <Title>{table.name}</Title>
          <Text>
            Table in &nbsp;
            <span>
              <Link to={Urls.browseDatabase({ id: table.database_id })}>
                <Database.Name id={table.database_id} />&nbsp;
              </Link>
              {table.table_schema && (
                <span>
                  <Icon name="chevronright" mx="4px" size={10} />
                  {/* we have to do some {} manipulation here to make this look like the table object that browseSchema was written for originally */}
                  <Link
                    to={Urls.browseSchema({
                      db: { id: table.database_id },
                      schema_name: table.table_schema,
                    })}
                  >
                    {table.table_schema}
                  </Link>
                </span>
              )}
            </span>
          </Text>
        </Box>
      </Flex>
    </ResultLink>
  );
}

const CollectionLink = styled(Link)`
  text-decoration: dashed;
  &:hover {
    color: ${color("brand")};
  }
`;

function CollectionBadge({ collection }) {
  return (
    <CollectionLink to={Urls.collection(collection.id)}>
      {collection.name}
    </CollectionLink>
  );
}

const Title = styled("h3")`
  margin-bottom: 4px;
`;

function Score({ scores }) {
  return (
    <pre className="hide search-score">{JSON.stringify(scores, null, 2)}</pre>
  );
}

// Generally these should be at the bottom of the list I'd hope
function CollectionResult({ collection, options }) {
  return (
    <ResultLink to={Urls.collection(collection.id)} compact={options.compact}>
      <Flex align="start">
        <ItemIcon item={collection} type="collection" />
        <Box>
          <Title>{collection.name}</Title>
          <Text>{jt`Collection in ${formatCollection(
            collection.getCollection(),
          )}`}</Text>
        </Box>
        <Score scores={collection.scores} />
      </Flex>
    </ResultLink>
  );
}

function contextText(context) {
  return context.map(function({ is_match, text }) {
    if (is_match) {
      return <strong style={{ color: color("brand") }}> {text}</strong>;
    } else {
      return <span> {text}</span>;
    }
  });
}

const Context = styled("p")`
  line-height: 1.4em;
  color: ${color("text-medium")};
  margin-top: 0;
`;

function formatContext(context, compact) {
  return (
    !compact &&
    context && (
      <Box ml="42px" mt="12px" style={{ maxWidth: 620 }}>
        <Context>{contextText(context)}</Context>
      </Box>
    )
  );
}

function formatCollection(collection) {
  return collection.id && <CollectionBadge collection={collection} />;
}

const Description = styled(Text)`
  padding-left: 8px;
  margin-top: 6px !important;
  border-left: 2px solid ${lighten("brand", 0.45)};
`;

function DashboardResult({ dashboard, options }) {
  return (
    <ResultLink to={dashboard.getUrl()} compact={options.compact}>
      <Flex align="start">
        <ItemIcon item={dashboard} type="dashboard" />
        <Box>
          <Title>{dashboard.name}</Title>
          <Text>{jt`Dashboard in ${formatCollection(
            dashboard.getCollection(),
          )}`}</Text>
          {dashboard.description && (
            <Description>{dashboard.description}</Description>
          )}
          <Score scores={dashboard.scores} />
        </Box>
      </Flex>
      {formatContext(dashboard.context, options.compact)}
    </ResultLink>
  );
}

function QuestionResult({ question, options }) {
  return (
    <ResultLink to={question.getUrl()} compact={options.compact}>
      <Flex align="start">
        <ItemIcon item={question} type="question" />
        <Box>
          <Title>{question.name}</Title>
          <Text>{jt`Saved question in ${formatCollection(
            question.getCollection(),
          )}`}</Text>
          {question.description && (
            <Description>{question.description}</Description>
          )}
          <Score scores={question.scores} />
        </Box>
      </Flex>
      {formatContext(question.context, options.compact)}
    </ResultLink>
  );
}

function SegmentResult({ segment, options }) {
  return (
    <ResultLink to={segment.getUrl()} compact={options.compact}>
      <Flex align="start">
        <ItemIcon item={segment} />
        <Box>
          <Title>{segment.name}</Title>
          <Text>{jt`Segment of ${(
            <Link
              to={Urls.tableRowsQuery(segment.database_id, segment.table_id)}
            >
              <Table.Name id={segment.table_id} />
            </Link>
          )}`}</Text>
          {segment.description && (
            <Description>{segment.description}</Description>
          )}
          <Score scores={segment.scores} />
        </Box>
      </Flex>
    </ResultLink>
  );
}

function MetricResult({ metric, options }) {
  return (
    <ResultLink to={metric.getUrl()} compact={options.compact}>
      <Flex align="start">
        <ItemIcon item={metric} />
        <Box>
          <Title>{metric.name}</Title>
          <Text>{jt`Metric for ${(
            <Link to={Urls.tableRowsQuery(metric.database_id, metric.table_id)}>
              <Table.Name id={metric.table_id} />
            </Link>
          )}`}</Text>
          {metric.description && (
            <Description>{metric.description}</Description>
          )}
          <Score scores={metric.scores} />
        </Box>
      </Flex>
    </ResultLink>
  );
}

function DefaultResult({ result, options }) {
  return (
    <ResultLink to={result.getUrl()} compact={options.compact}>
      <Flex align="start">
        <ItemIcon item={result} />
        <Box>
          <Title>{result.name}</Title>
          <Text>{jt`${result.model} in ${formatCollection(
            result.getCollection(),
          )}`}</Text>
          {result.description && (
            <Description>{result.description}</Description>
          )}
          <Score scores={result.scores} />
        </Box>
      </Flex>
      {formatContext(result.context, options.compact)}
    </ResultLink>
  );
}
