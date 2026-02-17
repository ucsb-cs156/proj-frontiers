import OurTable from "main/components/OurTable";

const columns = [
  {
    header: "SHA",
    accessorKey: "sha",
    id: "sha",
    cell: ({ cell }) => cell.getValue()?.substring(0, 7),
  },
  {
    header: "Message",
    accessorKey: "message",
    id: "message",
  },
  {
    header: "Commit Time",
    accessorKey: "commitTime",
    id: "commitTime",
    cell: ({ cell }) => {
      const value = cell.getValue();
      return value ? new Date(value).toLocaleString() : "";
    },
  },
  {
    header: "Committer Name",
    accessorKey: "committerName",
    id: "committerName",
  },
  {
    header: "Committer Email",
    accessorKey: "committerEmail",
    id: "committerEmail",
  },
  {
    header: "Committer Login",
    accessorKey: "committerLogin",
    id: "committerLogin",
  },
  {
    header: "Author Name",
    accessorKey: "authorName",
    id: "authorName",
  },
  {
    header: "Author Email",
    accessorKey: "authorEmail",
    id: "authorEmail",
  },
  {
    header: "Author Login",
    accessorKey: "authorLogin",
    id: "authorLogin",
  },
  {
    header: "URL",
    accessorKey: "url",
    id: "url",
  },
];

export default function AggregatedCommitsTable({ commits }) {
  return (
    <OurTable
      data={commits}
      columns={columns}
      testid={"AggregatedCommitsTable"}
    />
  );
}
