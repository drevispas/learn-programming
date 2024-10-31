import {Button, Grid, Grid2, TextField} from "@mui/material";

export const AddTodo = (props) => {
    return (
        <Grid container={true} style={{marginTop: 20}}>
            <Grid xs={11} md={11} item style={{paddingRight: 16}}>
                <TextField placeholder={"Add Todo"} fullWidth={true}/>
            </Grid>
            <Grid xs={1} md={1} item>
                <Button fullWidth style={{height: '100%'}} color={"secondary"} variant={"outlined"}>+</Button>
            </Grid>
        </Grid>
    )
}
