import React, { useState, useEffect } from 'react';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Box from '@mui/material/Box';
import { DataGrid } from '@mui/x-data-grid';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import Typography from '@mui/material/Typography';
import Avatar from '@mui/material/Avatar';
import Link from "@mui/material/Link";

function App() {
    const [data, setData] = useState([]);
    const [data2, setData2] = useState([]);
    const [keyword, setKeyword] = useState('');

    // GET with fetch API
    useEffect(() => {
        var url = 'https://stream.wikimedia.org/v2/stream/recentchange';

        const fetchData = async () => {
            const response = await fetch(
                url
            );
            const data = await response.json();
            console.log(data);
            setData(data);
        };
        // fetchData();

        const eventSource = new EventSource(url);

        eventSource.onopen = () => {
            console.log('SSE connection established');
        };

        eventSource.onmessage = (event) => {
            try {
                const newData = JSON.parse(event.data);
                // const newData = event.data;
                setData2(newData);
                // console.log(newData.id)
            } catch (error) {
                console.error('Error Streaming Event: ', error);
            }
        };

        eventSource.onerror = (error) => {
            console.error('SSE error:', error);
        };



    }, []);

    const columns = [
        { field: 'id', headerName: 'ID', width: 150 },
        // {
        //     field: 'logo',
        //     headerName: 'Logo',
        //     width: 100,
        //     editable: true,
        //     renderCell: (params) =>
        //         <Link href={params.row.logo}>
        //             <Avatar alt={params.row.name} variant="rounded" src={params.row.logo} />
        //         </Link>
        // },
        // {
        //     field: 'ticker',
        //     headerName: 'Ticker',
        //     width: 100,
        //     editable: true,
        // },
        // {
        //     field: 'date',
        //     headerName: 'Date',
        //     width: 100,
        //     editable: true,
        // },
        // {
        //     field: 'volume',
        //     headerName: 'Volume',
        //     type: 'number',
        //     width: 120,
        //     editable: true,
        // },
        // {
        //     field: 'open',
        //     headerName: 'Open',
        //     type: 'number',
        //     width: 100,
        //     editable: true,
        // },
        // {
        //     field: 'close',
        //     headerName: 'Close',
        //     type: 'number',
        //     width: 100,
        //     editable: true,
        // },
        // {
        //     field: "status",
        //     headerName: "Status",
        //     sortable: false,
        //     headerAlign: 'center',
        //     align: 'center',
        //     width: 150,
        //     disableClickEventBubbling: true,
        //     renderCell: (params) => {
        //         return (
        //             <div
        //                 className="d-flex justify-content-between align-items-center"
        //                 style={{ cursor: "pointer" }}
        //             >
        //                 <MatUpDown id={params.row.id} index={params.row} />
        //             </div>
        //         );
        //     }
        // }
    ];

    const MatUpDown = ({ index }) => {
        if (index.status.toUpperCase() === "UP") {
            return (
                <Box sx={{
                    display: 'flex',
                    color: 'success.main'
                }}>
                    <ArrowUpwardIcon sx={{ m: 0.5 }} />
                    <Typography sx={{ m: 0.5 }} variant="button" display="block" gutterBottom>
                        {index.changeval}
                    </Typography>
                    <Typography sx={{ m: 0.5 }} variant="button" display="block" gutterBottom>
                        {index.changepercent}
                    </Typography>

                </Box>
            );
        } else {
            return (
                <Box sx={{
                    display: 'flex',
                    color: 'error.main'
                }}>
                    <ArrowDownwardIcon sx={{ m: 0.5 }} />
                    <Typography sx={{ m: 0.5 }} variant="button" display="block" gutterBottom>
                        {index.changeval}
                    </Typography>
                    <Typography sx={{ m: 0.5 }} variant="button" display="block" gutterBottom>
                        {index.changepercent}
                    </Typography>
                </Box>
            );
        }
    };

    const handleRowClick: GridEventListener<'rowClick'> = (params) => {
        window.location.href = 'https://google.com';
    };

    return (
        <Box sx={{ flexGrow: 1 }}>
            {/* <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'flex-start',
                    p: 1,
                    m: 1,
                    bgcolor: 'background.paper',
                    borderRadius: 1,
                }}>
                <form onSubmit={handleSubmit}>
                    <Box component="span" sx={{ p: 0 }}>
                        <TextField size="small" id="outlined-basic" label="Ticker Name" variant="outlined" type="text" className="form-control" id="" value={keyword}
                            onChange={(e) => setKeyword(e.target.value)}
                        />
                    </Box>
                    <Box component="span" sx={{ p: 1 }}>
                        <Button size="medium" variant="contained" type="submit">Search History Ticker</Button>
                    </Box>
                </form>
            </Box> */}
            <Box sx={{ height: 480, width: '100%' }}>
                <DataGrid
                    onRowClick={handleRowClick}
                    rows={data2}
                    columns={columns}
                    initialState={{
                        // columns: {
                        //     columnVisibilityModel: {
                        //         id: false,
                        //     }
                        // },
                        pagination: {
                            paginationModel: {
                                pageSize: 7,
                            },
                        },
                    }}
                    pageSizeOptions={[5]}
                    checkboxSelection
                    disableRowSelectionOnClick
                />
            </Box>
        </Box >
    );
};

export default App;
